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
[Route("api/v1/offers")]
[Authorize]
public class OffersController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly INotificationService _notifications;

    public OffersController(AppDbContext db, INotificationService notifications)
    {
        _db = db;
        _notifications = notifications;
    }

    // POST /api/v1/offers - submits a purchase/lend/trade offer for a listing.
    [HttpPost]
    public async Task<ActionResult<OfferResponse>> Create(CreateOfferRequest request, CancellationToken ct)
    {
        var buyerId = User.GetUserId();

        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingId == request.ListingId, ct);
        if (listing == null) return NotFound(new ApiMessage("Listing not found."));
        if (listing.Status != "Active") return Conflict(new ApiMessage("This listing is no longer available."));
        if (listing.SellerId == buyerId) return BadRequest(new ApiMessage("You can't make an offer on your own listing."));
        if (request.OfferedPrice < 0 || request.OfferedPrice > 1_000_000)
            return BadRequest(new ApiMessage("Please enter a valid offer amount."));
        if (listing.ListingType == "Sell" && request.OfferedPrice == 0)
            return BadRequest(new ApiMessage("Please enter an offer above R0."));

        // Re-use the buyer's existing pending offer (without a meeting) instead of creating duplicates.
        var existing = await _db.Offers
            .Where(o => o.ListingId == listing.ListingId && o.BuyerId == buyerId && o.Status == "Pending")
            .Where(o => !_db.TradeMeetings.Any(t => t.OfferId == o.OfferId))
            .FirstOrDefaultAsync(ct);

        Offer offer;
        if (existing != null)
        {
            existing.OfferedPrice = request.OfferedPrice;
            offer = existing;
        }
        else
        {
            offer = new Offer { ListingId = listing.ListingId, BuyerId = buyerId, OfferedPrice = request.OfferedPrice };
            _db.Offers.Add(offer);
        }
        await _db.SaveChangesAsync(ct);

        var buyer = await _db.Users.FindAsync(new object[] { buyerId }, ct);
        await _notifications.NotifyAsync(listing.SellerId, "offer", "New offer received",
            $"{buyer?.FullName ?? "A student"} offered R{request.OfferedPrice:0.00} for \"{listing.Title}\".",
            listingId: listing.ListingId, ct: ct);

        return Ok(new OfferResponse { OfferId = offer.OfferId, Status = offer.Status });
    }
}
