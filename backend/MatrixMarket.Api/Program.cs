using System.Text;
using MatrixMarket.Api.Data;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;

// ---------------------------------------------------------------------------
// Matrix Market REST API - composition root.
// Wires up: SQLite (relational), LiteDB (NoSQL), Blob storage, JWT auth,
// Firebase token verification, FCM push notifications and Swagger.
// ---------------------------------------------------------------------------
var builder = WebApplication.CreateBuilder(args);
var config = builder.Configuration;

// Where the database files live. On Azure App Service /home is the persistent
// share, so we default there; locally we use an App_Data folder.
var dataDirectory = StoragePaths.ResolveDataDirectory(config, builder.Environment.ContentRootPath);
Directory.CreateDirectory(dataDirectory);

// ---- Relational database (EF Core + SQLite) ----
var sqlConnection = config.GetConnectionString("Default")
                    ?? $"Data Source={Path.Combine(dataDirectory, "matrixmarket.db")}";
builder.Services.AddDbContext<AppDbContext>(options => options.UseSqlite(sqlConnection));

// ---- NoSQL document database (LiteDB) ----
// One shared LiteDatabase instance per process is the thread-safe usage pattern.
builder.Services.AddSingleton(_ => new LiteDB.LiteDatabase($"Filename={Path.Combine(dataDirectory, "matrixmarket-nosql.db")};Connection=direct"));
builder.Services.AddSingleton<INoSqlStore, LiteDbNoSqlStore>();

// ---- Blob storage (Azure Blob when configured, local disk otherwise) ----
var blobConnection = config["AzureBlob:ConnectionString"];
if (string.IsNullOrWhiteSpace(blobConnection))
    blobConnection = Environment.GetEnvironmentVariable("AZURE_STORAGE_CONNECTION_STRING");

if (!string.IsNullOrWhiteSpace(blobConnection))
{
    var containerName = config["AzureBlob:ContainerName"] ?? "listing-images";
    builder.Services.AddSingleton<IBlobStorageService>(sp =>
        new AzureBlobStorageService(blobConnection!, containerName, sp.GetRequiredService<ILogger<AzureBlobStorageService>>()));
}
else
{
    var mediaDirectory = Path.Combine(dataDirectory, "media");
    builder.Services.AddSingleton<IBlobStorageService>(sp =>
        new LocalBlobStorageService(mediaDirectory, sp.GetRequiredService<ILogger<LocalBlobStorageService>>()));
}

// ---- Push notifications (FCM) ----
builder.Services.AddSingleton<IPushSender>(sp =>
    FcmPushSender.CreateOrNoOp(config, sp.GetRequiredService<ILoggerFactory>()));
builder.Services.AddScoped<INotificationService, NotificationService>();

// ---- Domain services ----
builder.Services.AddScoped<TokenService>();
builder.Services.AddScoped<KarmaService>();
builder.Services.AddSingleton<IFirebaseTokenVerifier, FirebaseTokenVerifier>();
builder.Services.AddHttpClient<IBookLookupService, GoogleBooksLookupService>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(8);
});

// ---- Authentication: our own JWT bearer tokens ----
var jwtKey = config["Jwt:Key"];
if (string.IsNullOrWhiteSpace(jwtKey) || jwtKey.Length < 32)
    throw new InvalidOperationException("Jwt:Key must be configured and at least 32 characters long.");

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = config["Jwt:Issuer"],
            ValidateAudience = true,
            ValidAudience = config["Jwt:Audience"],
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(2)
        };
    });
builder.Services.AddAuthorization();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo { Title = "Matrix Market API", Version = "v1" });
    var scheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Paste the token returned by /api/v1/auth/login",
        Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "Bearer" }
    };
    options.AddSecurityDefinition("Bearer", scheme);
    options.AddSecurityRequirement(new OpenApiSecurityRequirement { { scheme, Array.Empty<string>() } });
});

// Azure App Service terminates HTTPS at its front end and forwards over HTTP.
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

var app = builder.Build();

// ---- Create + seed the relational database on startup ----
using (var scope = app.Services.CreateScope())
{
    var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
    try
    {
        var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        db.Database.EnsureCreated();
        DbSeeder.Seed(db);
        logger.LogInformation("Database ready at {Directory}", dataDirectory);
    }
    catch (Exception ex)
    {
        logger.LogError(ex, "Database initialisation failed");
    }
}

app.UseForwardedHeaders();

// Swagger stays on in production so the API can be demonstrated live.
app.UseSwagger();
app.UseSwaggerUI();

app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.MapGet("/", () => Results.Redirect("/swagger"));

app.Run();

// Exposed so integration tests can reference the entry point if needed.
public partial class Program { }
