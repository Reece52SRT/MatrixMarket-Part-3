using System.Security.Cryptography;
using MatrixMarket.Api.Data;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Extensions;
using MatrixMarket.Api.Models;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

// Trade Meeting Scheduler (User Defined Feature 2).
// Lecturer feedback applied:
//   * Only the buyer or seller of a trade may schedule, view or confirm it.
//   * A trade can only be confirmed once, so karma is awarded once per trade.
// Flow: buyer schedules -> buyer receives a 6-digit code -> at the meeting the
// buyer shows the code -> the SELLER types it in to confirm the hand-over.
[ApiController]
[Route("api/v1/trades")]
[Authorize]
public class TradesController : ControllerBase
{
    // South African campus time (UTC+2, no daylight saving).
    private static readonly TimeSpan CampusOffset = TimeSpan.FromHours(2);

    private readonly AppDbContext _db;
    private readonly KarmaService _karmaService;
    private readonly INotificationService _notifications;
    private readonly INoSqlStore _noSql;
    private readonly ILogger<TradesController> _logger;

    public TradesController(AppDbContext db, KarmaService karmaService, INotificationService notifications,
        INoSqlStore noSql, ILogger<TradesController> logger)
    {
        _db = db;
        _karmaService = karmaService;
        _notifications = notifications;
        _noSql = noSql;
        _logger = logger;
    }

    // GET /api/v1/trades/mine - every meeting the logged-in user is part of.
    [HttpGet("mine")]
    public async Task<ActionResult<List<MyTradeMeetingResponse>>> GetMine()
    {
        var userId = User.GetUserId();

        var meetings = await _db.TradeMeetings
            .Include(t => t.Offer).ThenInclude(o => o!.Listing).ThenInclude(l => l!.Seller)
            .Include(t => t.Offer).ThenInclude(o => o!.Buyer)
            .Where(t => t.Offer != null &&
                        (t.Offer.BuyerId == userId || t.Offer.Listing!.SellerId == userId))
            .OrderBy(t => t.IsConfirmed).ThenBy(t => t.MeetingTime)
            .ToListAsync();

        var result = meetings.Select(m =>
        {
            var isBuyer = m.Offer!.BuyerId == userId;
            return new MyTradeMeetingResponse
            {
                MeetingId = m.MeetingId,
                OfferId = m.OfferId,
                CampusSpot = m.CampusSpot,
                MeetingTime = m.MeetingTime,
                ConfirmationCode = isBuyer ? m.ConfirmationCode : string.Empty,
                IsConfirmed = m.IsConfirmed,
                CanConfirm = !isBuyer && !m.IsConfirmed,
                ListingTitle = m.Offer.Listing?.Title ?? "",
                Price = m.Offer.OfferedPrice,
                Role = isBuyer ? "Buyer" : "Seller",
                OtherPartyName = isBuyer
                    ? (m.Offer.Listing?.Seller?.FullName ?? "Seller")
                    : (m.Offer.Buyer?.FullName ?? "Buyer")
            };
        }).ToList();

        return Ok(result);
    }

    // POST /api/v1/trades/schedule - buyer or seller of the offer only.
    [HttpPost("schedule")]
    public async Task<ActionResult<TradeMeetingResponse>> Schedule(ScheduleTradeRequest request, CancellationToken ct)
    {
        var userId = User.GetUserId();
        var offer = await _db.Offers.Include(o => o.Listing)
            .FirstOrDefaultAsync(o => o.OfferId == request.OfferId, ct);
        if (offer?.Listing == null) return NotFound(new ApiMessage("Offer not found."));

        var isBuyer = offer.BuyerId == userId;
        var isSeller = offer.Listing.SellerId == userId;
        if (!isBuyer && !isSeller)
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("You are not part of this trade."));

        if (offer.Status is "Declined" or "Completed")
            return Conflict(new ApiMessage("This offer can no longer be scheduled."));
        if (await _db.TradeMeetings.AnyAsync(t => t.OfferId == offer.OfferId, ct))
            return Conflict(new ApiMessage("A meeting has already been scheduled for this offer."));
        if (offer.Listing.Status != "Active")
            return Conflict(new ApiMessage("This listing is already reserved for another trade."));

        var spot = (request.CampusSpot ?? string.Empty).Trim();
        if (spot.Length < 3 || spot.Length > 100) return BadRequest(new ApiMessage("Please choose a campus meeting spot."));

        var campusNow = DateTime.UtcNow + CampusOffset;
        if (request.MeetingTime < campusNow.AddMinutes(-5))
            return BadRequest(new ApiMessage("The meeting time must be in the future."));
        if (request.MeetingTime > campusNow.AddDays(60))
            return BadRequest(new ApiMessage("Please choose a meeting within the next 60 days."));

        var meeting = new TradeMeeting
        {
            OfferId = offer.OfferId,
            CampusSpot = spot,
            MeetingTime = DateTime.SpecifyKind(request.MeetingTime, DateTimeKind.Unspecified),
            ConfirmationCode = RandomNumberGenerator.GetInt32(100000, 1000000).ToString()
        };

        _db.TradeMeetings.Add(meeting);
        offer.Status = "Accepted";
        offer.Listing.Status = "Reserved"; // hide it from the feed while the trade is in progress
        await _db.SaveChangesAsync(ct);
        _logger.LogInformation("Meeting {MeetingId} scheduled for offer {OfferId}", meeting.MeetingId, offer.OfferId);

        var otherUserId = isBuyer ? offer.Listing.SellerId : offer.BuyerId;
        await _notifications.NotifyAsync(otherUserId, "meeting", "Trade meeting scheduled",
            $"\"{offer.Listing.Title}\" - {spot}, {meeting.MeetingTime:ddd d MMM HH:mm}.",
            listingId: offer.ListingId, meetingId: meeting.MeetingId, ct: ct);

        return Ok(ToResponse(meeting, offer.Listing.Title, includeCode: isBuyer));
    }

    // GET /api/v1/trades/{meetingId} - buyer or seller only.
    [HttpGet("{meetingId:int}")]
    public async Task<ActionResult<TradeMeetingResponse>> GetMeeting(int meetingId)
    {
        var userId = User.GetUserId();
        var meeting = await _db.TradeMeetings
            .Include(t => t.Offer).ThenInclude(o => o!.Listing)
            .FirstOrDefaultAsync(t => t.MeetingId == meetingId);

        if (meeting?.Offer?.Listing == null) return NotFound(new ApiMessage("Meeting not found."));

        var isBuyer = meeting.Offer.BuyerId == userId;
        var isSeller = meeting.Offer.Listing.SellerId == userId;
        if (!isBuyer && !isSeller)
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("You are not part of this trade."));

        return Ok(ToResponse(meeting, meeting.Offer.Listing.Title, includeCode: isBuyer));
    }

    // POST /api/v1/trades/{meetingId}/confirm - the SELLER enters the buyer's code.
    [HttpPost("{meetingId:int}/confirm")]
    public async Task<IActionResult> Confirm(int meetingId, ConfirmTradeRequest request, CancellationToken ct)
    {
        var userId = User.GetUserId();
        var meeting = await _db.TradeMeetings
            .Include(t => t.Offer).ThenInclude(o => o!.Listing)
            .FirstOrDefaultAsync(t => t.MeetingId == meetingId, ct);

        if (meeting?.Offer?.Listing == null) return NotFound(new ApiMessage("Meeting not found."));

        var offer = meeting.Offer;
        var listing = offer.Listing!;
        var isBuyer = offer.BuyerId == userId;
        var isSeller = listing.SellerId == userId;

        if (!isBuyer && !isSeller)
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("You are not part of this trade."));
        if (meeting.IsConfirmed)
            return Conflict(new ApiMessage("This trade has already been confirmed."));
        if (!isSeller)
            return StatusCode(StatusCodes.Status403Forbidden,
                new ApiMessage("Only the seller can confirm the hand-over. Show the seller your code."));
        if ((request.ConfirmationCode ?? string.Empty).Trim() != meeting.ConfirmationCode)
            return BadRequest(new ApiMessage("Incorrect confirmation code."));

        // Atomic "confirm only if not already confirmed": if two requests race,
        // only one of them can flip the flag, so karma is awarded exactly once.
        var updated = await _db.TradeMeetings
            .Where(t => t.MeetingId == meetingId && !t.IsConfirmed)
            .ExecuteUpdateAsync(s => s.SetProperty(t => t.IsConfirmed, true), ct);
        if (updated == 0)
            return Conflict(new ApiMessage("This trade has already been confirmed."));

        listing.Status = "Sold";
        offer.Status = "Completed";
        await _db.SaveChangesAsync(ct);

        await _karmaService.AwardTradeCompletionAsync(listing.SellerId, offer.BuyerId);
        _logger.LogInformation("Meeting {MeetingId} confirmed; karma awarded", meetingId);

        await _notifications.NotifyAsync(offer.BuyerId, "trade_confirmed", "Trade complete",
            $"Your trade for \"{listing.Title}\" is confirmed. +{KarmaService.BuyerTradePoints} Student Score!",
            listingId: listing.ListingId, meetingId: meetingId, ct: ct);
        await _notifications.NotifyAsync(listing.SellerId, "trade_confirmed", "Trade complete",
            $"You sold \"{listing.Title}\". +{KarmaService.SellerTradePoints} Student Score!",
            listingId: listing.ListingId, meetingId: meetingId, ct: ct);

        // Let anyone watching this listing know it's gone.
        foreach (var watcherId in _noSql.GetWatchers(listing.ListingId)
                     .Where(id => id != offer.BuyerId && id != listing.SellerId))
        {
            await _notifications.NotifyAsync(watcherId, "listing_sold", "Saved item sold",
                $"\"{listing.Title}\" from your watchlist has been sold.", listingId: listing.ListingId, ct: ct);
        }

        return Ok(new { isConfirmed = true, message = "Trade confirmed. Karma points awarded." });
    }

    private static TradeMeetingResponse ToResponse(TradeMeeting m, string title, bool includeCode) => new()
    {
        MeetingId = m.MeetingId,
        OfferId = m.OfferId,
        CampusSpot = m.CampusSpot,
        MeetingTime = m.MeetingTime,
        ConfirmationCode = includeCode ? m.ConfirmationCode : string.Empty,
        IsConfirmed = m.IsConfirmed,
        ListingTitle = title
    };
}
