using MatrixMarket.Api.Data;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Extensions;
using MatrixMarket.Api.Models;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

[ApiController]
[Route("api/v1/listings")]
public class ListingsController : ControllerBase
{
    private static readonly string[] ListingTypes = { "Sell", "Trade", "Lend" };
    private static readonly string[] AllowedImageTypes = { "image/jpeg", "image/png", "image/webp" };
    private const long MaxImageBytes = 5 * 1024 * 1024;

    private readonly AppDbContext _db;
    private readonly IBlobStorageService _blobs;
    private readonly INoSqlStore _noSql;
    private readonly ILogger<ListingsController> _logger;

    public ListingsController(AppDbContext db, IBlobStorageService blobs, INoSqlStore noSql, ILogger<ListingsController> logger)
    {
        _db = db;
        _blobs = blobs;
        _noSql = noSql;
        _logger = logger;
    }

    // GET /api/v1/listings?category={id}&search={term}&type={sell|lend|trade}
    [HttpGet]
    public async Task<ActionResult<List<ListingResponse>>> GetListings(
        [FromQuery] int? category, [FromQuery] string? search, [FromQuery] string? type)
    {
        var query = _db.Listings
            .Include(l => l.Category)
            .Include(l => l.Seller)
            .Where(l => l.Status == "Active");

        if (category.HasValue) query = query.Where(l => l.CategoryId == category.Value);
        if (!string.IsNullOrWhiteSpace(type))
        {
            var t = type.Trim().ToLower();
            query = query.Where(l => l.ListingType.ToLower() == t);
        }
        if (!string.IsNullOrWhiteSpace(search))
        {
            var s = search.Trim().ToLower();
            query = query.Where(l => l.Title.ToLower().Contains(s) || l.Description.ToLower().Contains(s)
                                     || (l.Isbn != null && l.Isbn.Contains(s)));
        }

        var results = await query.OrderByDescending(l => l.CreatedAt).ToListAsync();
        return Ok(results.Select(ToResponse).ToList());
    }

    // GET /api/v1/listings/{id}
    [HttpGet("{id:int}")]
    public async Task<ActionResult<ListingResponse>> GetById(int id)
    {
        var listing = await _db.Listings
            .Include(l => l.Category).Include(l => l.Seller)
            .FirstOrDefaultAsync(l => l.ListingId == id);

        return listing == null ? NotFound(new ApiMessage("Listing not found.")) : Ok(ToResponse(listing));
    }

    // POST /api/v1/listings (requires auth)
    [HttpPost]
    [Authorize]
    public async Task<ActionResult<ListingResponse>> Create(CreateListingRequest request)
    {
        var sellerId = User.GetUserId();
        var title = (request.Title ?? string.Empty).Trim();
        var description = (request.Description ?? string.Empty).Trim();
        var listingType = ListingTypes.FirstOrDefault(t => t.Equals(request.ListingType?.Trim(), StringComparison.OrdinalIgnoreCase));

        if (title.Length < 3 || title.Length > 120) return BadRequest(new ApiMessage("Title must be 3-120 characters."));
        if (description.Length < 5 || description.Length > 2000) return BadRequest(new ApiMessage("Description must be 5-2000 characters."));
        if (listingType == null) return BadRequest(new ApiMessage("Listing type must be Sell, Trade or Lend."));
        if (request.Price < 0 || request.Price > 1_000_000) return BadRequest(new ApiMessage("Please enter a valid price."));
        if (listingType == "Sell" && request.Price == 0) return BadRequest(new ApiMessage("Items for sale need a price above R0."));
        if (!await _db.Categories.AnyAsync(c => c.CategoryId == request.CategoryId))
            return BadRequest(new ApiMessage("Please choose a valid category."));

        string? isbn = null;
        if (!string.IsNullOrWhiteSpace(request.Isbn))
        {
            if (!IsbnValidator.IsValid(request.Isbn))
                return BadRequest(new ApiMessage("That ISBN is not valid. Book ISBNs start with 978 or 979."));
            isbn = IsbnValidator.Normalize(request.Isbn);
        }

        var listing = new Listing
        {
            SellerId = sellerId,
            CategoryId = request.CategoryId,
            Title = title,
            Description = description,
            Price = request.Price,
            ListingType = listingType,
            Isbn = isbn,
            ImageUrl = request.ImageUrl
        };

        _db.Listings.Add(listing);
        await _db.SaveChangesAsync();

        await _db.Entry(listing).Reference(l => l.Category).LoadAsync();
        await _db.Entry(listing).Reference(l => l.Seller).LoadAsync();
        _logger.LogInformation("User {UserId} created listing {ListingId}", sellerId, listing.ListingId);

        return CreatedAtAction(nameof(GetById), new { id = listing.ListingId }, ToResponse(listing));
    }

    // POST /api/v1/listings/{id}/image (multipart/form-data, field "file") - seller only.
    // Stores the photo in blob storage (User Defined Feature 4).
    [HttpPost("{id:int}/image")]
    [Authorize]
    [RequestSizeLimit(MaxImageBytes + 1024 * 1024)]
    public async Task<ActionResult<ListingResponse>> UploadImage(int id, IFormFile? file, CancellationToken ct)
    {
        var listing = await _db.Listings.Include(l => l.Category).Include(l => l.Seller)
            .FirstOrDefaultAsync(l => l.ListingId == id, ct);
        if (listing == null) return NotFound(new ApiMessage("Listing not found."));
        if (listing.SellerId != User.GetUserId())
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("Only the seller can change this listing's photo."));

        if (file == null || file.Length == 0) return BadRequest(new ApiMessage("Please choose a photo."));
        if (file.Length > MaxImageBytes) return BadRequest(new ApiMessage("Photos must be smaller than 5 MB."));
        var contentType = (file.ContentType ?? string.Empty).ToLowerInvariant();
        if (!AllowedImageTypes.Contains(contentType)) return BadRequest(new ApiMessage("Only JPG, PNG or WEBP photos are allowed."));

        var extension = contentType switch { "image/png" => ".png", "image/webp" => ".webp", _ => ".jpg" };
        await using var stream = file.OpenReadStream();
        var blobName = await _blobs.UploadAsync(stream, contentType, extension, ct);

        // Remove the previous photo if there was one.
        var oldName = ExtractBlobName(listing.ImageUrl);
        if (oldName != null) await _blobs.DeleteAsync(oldName, ct);

        listing.ImageUrl = $"/api/v1/media/{blobName}";
        await _db.SaveChangesAsync(ct);
        return Ok(ToResponse(listing));
    }

    // DELETE /api/v1/listings/{id} (seller only, and only while still active)
    [HttpDelete("{id:int}")]
    [Authorize]
    public async Task<IActionResult> Delete(int id, CancellationToken ct)
    {
        var listing = await _db.Listings.FindAsync(new object[] { id }, ct);
        if (listing == null) return NotFound(new ApiMessage("Listing not found."));
        if (listing.SellerId != User.GetUserId())
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("You can only delete your own listings."));
        if (listing.Status != "Active")
            return Conflict(new ApiMessage("This listing has a trade in progress and can't be deleted."));

        var blobName = ExtractBlobName(listing.ImageUrl);
        _db.Listings.Remove(listing);
        await _db.SaveChangesAsync(ct);

        if (blobName != null) await _blobs.DeleteAsync(blobName, ct);
        _noSql.RemoveListingFromAllWatchlists(id);
        return NoContent();
    }

    private static string? ExtractBlobName(string? imageUrl)
    {
        const string prefix = "/api/v1/media/";
        if (string.IsNullOrWhiteSpace(imageUrl) || !imageUrl.StartsWith(prefix)) return null;
        var name = imageUrl[prefix.Length..];
        return BlobNames.IsSafe(name) ? name : null;
    }

    internal static ListingResponse ToResponse(Listing l) => new()
    {
        ListingId = l.ListingId,
        Title = l.Title,
        Description = l.Description,
        Price = l.Price,
        ListingType = l.ListingType,
        Isbn = l.Isbn,
        ImageUrl = l.ImageUrl,
        Status = l.Status,
        CategoryId = l.CategoryId,
        CategoryName = l.Category?.CategoryName ?? "",
        SellerName = l.Seller?.FullName ?? "",
        SellerId = l.SellerId,
        SellerKarma = l.Seller?.KarmaPoints ?? 0,
        CreatedAt = l.CreatedAt
    };
}
