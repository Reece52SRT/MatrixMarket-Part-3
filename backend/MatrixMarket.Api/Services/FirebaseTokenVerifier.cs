using System.IdentityModel.Tokens.Jwt;
using Microsoft.IdentityModel.Protocols;
using Microsoft.IdentityModel.Protocols.OpenIdConnect;
using Microsoft.IdentityModel.Tokens;

namespace MatrixMarket.Api.Services;

public record FirebaseIdentity(string Uid, string Email, string Name, bool EmailVerified, string Provider);

// Verifies a Firebase ID token SERVER-SIDE (lecturer feedback: "verify the provider
// token server-side before issuing an app token"). The token's signature is checked
// against Google's published public keys, and the issuer/audience must match our
// Firebase project, so a client can no longer just send us any email address.
public interface IFirebaseTokenVerifier
{
    Task<FirebaseIdentity?> VerifyAsync(string idToken, CancellationToken ct = default);
}

public class FirebaseTokenVerifier : IFirebaseTokenVerifier
{
    private readonly string _projectId;
    private readonly ConfigurationManager<OpenIdConnectConfiguration> _configManager;
    private readonly ILogger<FirebaseTokenVerifier> _logger;

    public FirebaseTokenVerifier(IConfiguration config, ILogger<FirebaseTokenVerifier> logger)
    {
        _projectId = config["Firebase:ProjectId"] ?? throw new InvalidOperationException("Firebase:ProjectId missing");
        _logger = logger;
        // Google publishes the signing keys for Firebase ID tokens here; the manager caches + refreshes them.
        _configManager = new ConfigurationManager<OpenIdConnectConfiguration>(
            $"https://securetoken.google.com/{_projectId}/.well-known/openid-configuration",
            new OpenIdConnectConfigurationRetriever(),
            new HttpDocumentRetriever { RequireHttps = true });
    }

    public async Task<FirebaseIdentity?> VerifyAsync(string idToken, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(idToken)) return null;

        try
        {
            var openId = await _configManager.GetConfigurationAsync(ct);
            var parameters = new TokenValidationParameters
            {
                ValidateIssuer = true,
                ValidIssuer = $"https://securetoken.google.com/{_projectId}",
                ValidateAudience = true,
                ValidAudience = _projectId,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.FromMinutes(5),
                ValidateIssuerSigningKey = true,
                IssuerSigningKeys = openId.SigningKeys
            };

            var handler = new JwtSecurityTokenHandler { MapInboundClaims = false };
            var principal = handler.ValidateToken(idToken, parameters, out _);

            var uid = principal.FindFirst("sub")?.Value ?? string.Empty;
            var email = principal.FindFirst("email")?.Value ?? string.Empty;
            var name = principal.FindFirst("name")?.Value ?? string.Empty;
            var verified = string.Equals(principal.FindFirst("email_verified")?.Value, "true", StringComparison.OrdinalIgnoreCase);
            var provider = "Google";

            if (string.IsNullOrWhiteSpace(uid) || string.IsNullOrWhiteSpace(email)) return null;
            return new FirebaseIdentity(uid, email.Trim().ToLowerInvariant(), name, verified, provider);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Firebase ID token rejected");
            return null;
        }
    }
}
