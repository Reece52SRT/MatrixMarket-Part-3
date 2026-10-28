namespace MatrixMarket.Api.DTOs;

public class RegisterRequest
{
    public string StudentEmail { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string? Campus { get; set; }
}

public class LoginRequest
{
    public string StudentEmail { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

// SSO login: the Android app signs in with Google through Firebase and sends us
// the Firebase ID TOKEN (not an email). The API verifies that token itself and
// reads the email from inside it.
public class SsoLoginRequest
{
    public string IdToken { get; set; } = string.Empty;
}

public class AuthResponse
{
    public string Token { get; set; } = string.Empty;
    public int UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string StudentEmail { get; set; } = string.Empty;
    public int KarmaPoints { get; set; }
    public string SsoProvider { get; set; } = "Email";
}
