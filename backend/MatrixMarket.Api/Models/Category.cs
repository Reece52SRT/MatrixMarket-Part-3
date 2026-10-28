namespace MatrixMarket.Api.Models;

// Academic category a listing belongs to (Textbooks, Electronics, ...).
public class Category
{
    public int CategoryId { get; set; }
    public string CategoryName { get; set; } = string.Empty;
    public string? Description { get; set; }
}
