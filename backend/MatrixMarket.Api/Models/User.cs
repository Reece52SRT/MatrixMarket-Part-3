namespace MatrixMarket.Api.Models;

// Represents a verified student user of Matrix Market.
public class User
{
    public int UserId { get; set; }
    public string StudentEmail { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string? Campus { get; set; }
    public int KarmaPoints { get; set; } = 0;
    public string SsoProvider { get; set; } = "Email"; // "Email" or "Google"
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<Listing> Listings { get; set; } = new List<Listing>();
    public ICollection<TrustBadge> Badges { get; set; } = new List<TrustBadge>();
}