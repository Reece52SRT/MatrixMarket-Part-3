using LiteDB;

namespace MatrixMarket.Api.Models;

// ---------------------------------------------------------------------------
// Documents stored in the NoSQL (LiteDB) database. These are schemaless JSON
// (BSON) documents rather than relational tables, which suits data that is
// append-heavy and read per-user (an inbox, a watchlist, device tokens).
// ---------------------------------------------------------------------------

// One entry in a user's in-app notification inbox.
public class NotificationDocument
{
    [BsonId] public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public int UserId { get; set; }
    public string Type { get; set; } = string.Empty; // offer, meeting, trade_confirmed, listing_sold, system
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public int? ListingId { get; set; }
    public int? MeetingId { get; set; }
    public bool IsRead { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

// The Firebase Cloud Messaging token of the user's phone (one device per user).
public class DeviceTokenDocument
{
    [BsonId] public int UserId { get; set; }
    public string Token { get; set; } = string.Empty;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

// A listing the user saved to their watchlist (User Defined Feature 5).
public class WatchlistDocument
{
    [BsonId] public string Id { get; set; } = string.Empty; // "{userId}:{listingId}"
    public int UserId { get; set; }
    public int ListingId { get; set; }
    public DateTime AddedAt { get; set; } = DateTime.UtcNow;

    public static string MakeId(int userId, int listingId) => $"{userId}:{listingId}";
}
