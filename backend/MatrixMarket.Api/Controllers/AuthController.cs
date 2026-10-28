using System.Text.RegularExpressions;
using MatrixMarket.Api.Data;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Models;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController : ControllerBase
{
    private static readonly Regex EmailPattern = new(@"^[^@\s]+@[^@\s]+\.[^@\s]+$", RegexOptions.Compiled);

    private readonly AppDbContext _db;
    private readonly TokenService _tokenService;
    private readonly IFirebaseTokenVerifier _firebaseVerifier;
    private readonly ILogger<AuthController> _logger;

    public AuthController(AppDbContext db, TokenService tokenService,
        IFirebaseTokenVerifier firebaseVerifier, ILogger<AuthController> logger)
    {
        _db = db;
        _tokenService = tokenService;
        _firebaseVerifier = firebaseVerifier;
        _logger = logger;
    }

    // POST /api/v1/auth/register - email + password account.
    [HttpPost("register")]
    public async Task<ActionResult<AuthResponse>> Register(RegisterRequest request)
    {
        var email = (request.StudentEmail ?? string.Empty).Trim().ToLowerInvariant();
        var fullName = (request.FullName ?? string.Empty).Trim();

        if (!EmailPattern.IsMatch(email)) return BadRequest(new ApiMessage("Please enter a valid email address."));
        if (string.IsNullOrEmpty(request.Password) || request.Password.Length < 6)
            return BadRequest(new ApiMessage("Password must be at least 6 characters."));
        if (fullName.Length < 2 || fullName.Length > 80)
            return BadRequest(new ApiMessage("Please enter your full name (2-80 characters)."));

        if (await _db.Users.AnyAsync(u => u.StudentEmail.ToLower() == email))
            return Conflict(new ApiMessage("An account with this email already exists."));

        var user = new User
        {
            StudentEmail = email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            FullName = fullName,
            Campus = string.IsNullOrWhiteSpace(request.Campus) ? null : request.Campus.Trim(),
            SsoProvider = "Email"
        };

        _db.Users.Add(user);
        await _db.SaveChangesAsync();
        _logger.LogInformation("Registered new user {UserId}", user.UserId);

        return Ok(BuildAuthResponse(user));
    }

    // POST /api/v1/auth/login
    [HttpPost("login")]
    public async Task<ActionResult<AuthResponse>> Login(LoginRequest request)
    {
        var email = (request.StudentEmail ?? string.Empty).Trim().ToLowerInvariant();
        if (email.Length == 0 || string.IsNullOrEmpty(request.Password))
            return BadRequest(new ApiMessage("Please enter your email and password."));

        var user = await _db.Users.FirstOrDefaultAsync(u => u.StudentEmail.ToLower() == email);
        if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            return Unauthorized(new ApiMessage("Invalid email or password."));

        return Ok(BuildAuthResponse(user));
    }

    // POST /api/v1/auth/sso-login
    // The client sends the Firebase ID token it received after Google Sign-In.
    // We VERIFY that token server-side (signature, issuer, audience, expiry) and only
    // then trust the email inside it. Creates the account on first login.
    [HttpPost("sso-login")]
    public async Task<ActionResult<AuthResponse>> SsoLogin(SsoLoginRequest request, CancellationToken ct)
    {
        var identity = await _firebaseVerifier.VerifyAsync(request.IdToken, ct);
        if (identity == null)
            return Unauthorized(new ApiMessage("Google sign-in could not be verified. Please try again."));
        if (!identity.EmailVerified)
            return Unauthorized(new ApiMessage("Your Google account email is not verified."));

        var user = await _db.Users.FirstOrDefaultAsync(u => u.StudentEmail.ToLower() == identity.Email, ct);
        if (user == null)
        {
            var name = string.IsNullOrWhiteSpace(identity.Name) ? identity.Email.Split('@')[0] : identity.Name.Trim();
            user = new User
            {
                StudentEmail = identity.Email,
                FullName = name.Length > 80 ? name[..80] : name,
                // Random unusable password: SSO accounts never sign in with a password.
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(Guid.NewGuid().ToString()),
                SsoProvider = identity.Provider
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync(ct);
            _logger.LogInformation("Created SSO user {UserId} for Firebase uid {Uid}", user.UserId, identity.Uid);
        }

        return Ok(BuildAuthResponse(user));
    }

    private AuthResponse BuildAuthResponse(User user) => new()
    {
        Token = _tokenService.GenerateToken(user),
        UserId = user.UserId,
        FullName = user.FullName,
        StudentEmail = user.StudentEmail,
        KarmaPoints = user.KarmaPoints,
        SsoProvider = user.SsoProvider
    };
}
