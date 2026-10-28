namespace MatrixMarket.Api.Models;

public class Listing
{
    public int ListingId { get; set; }
    public int SellerId { get; set; }
    public User? Seller { get; set; }
    public int CategoryId { get; set; }
    public Category? Category { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public decimal Price { get; set; }
    public string ListingType { get; set; } = "Sell"; // Sell, Trade, Lend
    public string? Isbn { get; set; }
    public string? ImageUrl { get; set; }
    public string Status { get; set; } = "Active"; // Active, Reserved, Sold
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}