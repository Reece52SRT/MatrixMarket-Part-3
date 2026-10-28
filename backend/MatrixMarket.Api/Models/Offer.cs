namespace MatrixMarket.Api.Models;

public class Offer
{
    public int OfferId { get; set; }
    public int ListingId { get; set; }
    public Listing? Listing { get; set; }
    public int BuyerId { get; set; }
    public User? Buyer { get; set; }
    public decimal OfferedPrice { get; set; }
    public string Status { get; set; } = "Pending"; // Pending, Accepted, Declined, Completed
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}