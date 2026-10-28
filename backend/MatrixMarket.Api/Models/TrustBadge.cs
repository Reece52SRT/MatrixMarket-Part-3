namespace MatrixMarket.Api.Models;

public class TrustBadge
{
    public int BadgeId { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }
    public string BadgeType { get; set; } = string.Empty; // e.g. "Top Seller", "Fast Responder"
    public DateTime AwardedAt { get; set; } = DateTime.UtcNow;
}