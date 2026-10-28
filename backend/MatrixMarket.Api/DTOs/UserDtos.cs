namespace MatrixMarket.Api.DTOs;

public class UserProfileResponse
{
    public int UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string StudentEmail { get; set; } = string.Empty;
    public string? Campus { get; set; }
    public int KarmaPoints { get; set; }
    public string TrustTier { get; set; } = string.Empty;
    public List<string> Badges { get; set; } = new();
    public int ListingsCount { get; set; }
    public DateTime MemberSince { get; set; }
    public int TradesCompleted { get; set; }
}

public class UpdateSettingsRequest
{
    public string? Campus { get; set; }
    public string? FullName { get; set; }
}

// Returned after a settings save so the app can refresh its cached session.
public class UpdateSettingsResponse
{
    public int UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string? Campus { get; set; }
}

public class DeviceTokenRequest
{
    public string Token { get; set; } = string.Empty;
}
