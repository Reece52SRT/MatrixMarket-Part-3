namespace MatrixMarket.Api.DTOs;

public class CreateListingRequest
{
    public int CategoryId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public decimal Price { get; set; }
    public string ListingType { get; set; } = "Sell";
    public string? Isbn { get; set; }
    public string? ImageUrl { get; set; }
}

public class ListingResponse
{
    public int ListingId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public decimal Price { get; set; }
    public string ListingType { get; set; } = string.Empty;
    public string? Isbn { get; set; }
    public string? ImageUrl { get; set; }
    public string Status { get; set; } = string.Empty;
    public int CategoryId { get; set; }
    public string CategoryName { get; set; } = string.Empty;
    public string SellerName { get; set; } = string.Empty;
    public int SellerId { get; set; }
    public int SellerKarma { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class BookInfoResponse
{
    public string Isbn { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public List<string> Authors { get; set; } = new();
    public string? Publisher { get; set; }
    public string? PublishedDate { get; set; }
    public string? Description { get; set; }
}
