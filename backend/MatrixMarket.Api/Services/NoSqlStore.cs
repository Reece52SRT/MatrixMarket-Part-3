using LiteDB;
using MatrixMarket.Api.Models;

namespace MatrixMarket.Api.Services;

// Abstraction over the NoSQL document database so controllers/tests don't care
// which engine is underneath.
public interface INoSqlStore
{
    // Notifications inbox
    void AddNotification(NotificationDocument notification);
    List<NotificationDocument> GetNotifications(int userId, int limit = 50);
    int CountUnread(int userId);
    bool MarkRead(int userId, string notificationId);
    int MarkAllRead(int userId);

    // FCM device tokens
    void SaveDeviceToken(int userId, string token);
    string? GetDeviceToken(int userId);
    void RemoveDeviceToken(int userId);
    void RemoveDeviceTokenValue(string token);

    // Watchlist
    void AddToWatchlist(int userId, int listingId);
    bool RemoveFromWatchlist(int userId, int listingId);
    List<int> GetWatchlistListingIds(int userId);
    List<int> GetWatchers(int listingId);
    void RemoveListingFromAllWatchlists(int listingId);
}

// LiteDB implementation - an embedded, serverless NoSQL document database.
public class LiteDbNoSqlStore : INoSqlStore
{
    private readonly ILiteCollection<NotificationDocument> _notifications;
    private readonly ILiteCollection<DeviceTokenDocument> _tokens;
    private readonly ILiteCollection<WatchlistDocument> _watchlist;

    public LiteDbNoSqlStore(LiteDatabase database)
    {
        _notifications = database.GetCollection<NotificationDocument>("notifications");
        _tokens = database.GetCollection<DeviceTokenDocument>("device_tokens");
        _watchlist = database.GetCollection<WatchlistDocument>("watchlist");

        _notifications.EnsureIndex(n => n.UserId);
        _watchlist.EnsureIndex(w => w.UserId);
        _watchlist.EnsureIndex(w => w.ListingId);
    }

    public void AddNotification(NotificationDocument notification) => _notifications.Insert(notification);

    public List<NotificationDocument> GetNotifications(int userId, int limit = 50) =>
        _notifications.Query()
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.CreatedAt)
            .Limit(limit)
            .ToList();

    public int CountUnread(int userId) => _notifications.Count(n => n.UserId == userId && !n.IsRead);

    public bool MarkRead(int userId, string notificationId)
    {
        var doc = _notifications.FindById(notificationId);
        if (doc == null || doc.UserId != userId) return false;
        doc.IsRead = true;
        return _notifications.Update(doc);
    }

    public int MarkAllRead(int userId)
    {
        var unread = _notifications.Find(n => n.UserId == userId && !n.IsRead).ToList();
        foreach (var doc in unread) doc.IsRead = true;
        return unread.Count == 0 ? 0 : _notifications.Update(unread);
    }

    public void SaveDeviceToken(int userId, string token)
    {
        // A token belongs to one phone - if another account used this phone before, detach it.
        RemoveDeviceTokenValue(token);
        _tokens.Upsert(new DeviceTokenDocument { UserId = userId, Token = token, UpdatedAt = DateTime.UtcNow });
    }

    public string? GetDeviceToken(int userId) => _tokens.FindById(userId)?.Token;

    public void RemoveDeviceToken(int userId) => _tokens.Delete(userId);

    public void RemoveDeviceTokenValue(string token) => _tokens.DeleteMany(t => t.Token == token);

    public void AddToWatchlist(int userId, int listingId) =>
        _watchlist.Upsert(new WatchlistDocument
        {
            Id = WatchlistDocument.MakeId(userId, listingId),
            UserId = userId,
            ListingId = listingId,
            AddedAt = DateTime.UtcNow
        });

    public bool RemoveFromWatchlist(int userId, int listingId) =>
        _watchlist.Delete(WatchlistDocument.MakeId(userId, listingId));

    public List<int> GetWatchlistListingIds(int userId) =>
        _watchlist.Query()
            .Where(w => w.UserId == userId)
            .OrderByDescending(w => w.AddedAt)
            .Select(w => w.ListingId)
            .ToList();

    public List<int> GetWatchers(int listingId) =>
        _watchlist.Find(w => w.ListingId == listingId).Select(w => w.UserId).ToList();

    public void RemoveListingFromAllWatchlists(int listingId) => _watchlist.DeleteMany(w => w.ListingId == listingId);
}
