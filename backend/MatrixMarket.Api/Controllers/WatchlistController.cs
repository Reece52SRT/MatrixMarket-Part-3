using MatrixMarket.Api.Data;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Extensions;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

// Saved Listings / Watchlist (User Defined Feature 5).
// Which listings a user saved is stored in the NoSQL database; the listing details
// themselves come from the relational database.
[ApiController]
[Route("api/v1/watchlist")]
[Authorize]
public class WatchlistController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly INoSqlStore _store;

    public WatchlistController(AppDbContext db, INoSqlStore store)
    {
        _db = db;
        _store = store;
    }

    // GET /api/v1/watchlist - full listing details, newest saved first.
    [HttpGet]
    public async Task<ActionResult<List<ListingResponse>>> Get()
    {
        var ids = _store.GetWatchlistListingIds(User.GetUserId());
        if (ids.Count == 0) return Ok(new List<ListingResponse>());

        var listings = await _db.Listings.Include(l => l.Category).Include(l => l.Seller)
            .Where(l => ids.Contains(l.ListingId))
            .ToListAsync();

        var ordered = ids.Select(id => listings.FirstOrDefault(l => l.ListingId == id))
            .Where(l => l != null)
            .Select(l => ListingsController.ToResponse(l!))
            .ToList();
        return Ok(ordered);
    }

    // GET /api/v1/watchlist/ids - lightweight list used to draw the heart icons.
    [HttpGet("ids")]
    public ActionResult<List<int>> GetIds() => Ok(_store.GetWatchlistListingIds(User.GetUserId()));

    // POST /api/v1/watchlist/{listingId}
    [HttpPost("{listingId:int}")]
    public async Task<IActionResult> Add(int listingId)
    {
        var listing = await _db.Listings.FindAsync(listingId);
        if (listing == null) return NotFound(new ApiMessage("Listing not found."));
        if (listing.SellerId == User.GetUserId()) return BadRequest(new ApiMessage("You can't save your own listing."));

        _store.AddToWatchlist(User.GetUserId(), listingId);
        return NoContent();
    }

    // DELETE /api/v1/watchlist/{listingId}
    [HttpDelete("{listingId:int}")]
    public IActionResult Remove(int listingId)
    {
        _store.RemoveFromWatchlist(User.GetUserId(), listingId);
        return NoContent();
    }
}
