using MatrixMarket.Api.Models;

namespace MatrixMarket.Api.Services;

// Creates a notification: always saved to the user's NoSQL inbox, and also
// pushed to their phone in real time through FCM when possible.
public interface INotificationService
{
    Task NotifyAsync(int userId, string type, string title, string body,
        int? listingId = null, int? meetingId = null, CancellationToken ct = default);
}

public class NotificationService : INotificationService
{
    private readonly INoSqlStore _store;
    private readonly IPushSender _push;
    private readonly ILogger<NotificationService> _logger;

    public NotificationService(INoSqlStore store, IPushSender push, ILogger<NotificationService> logger)
    {
        _store = store;
        _push = push;
        _logger = logger;
    }

    public async Task NotifyAsync(int userId, string type, string title, string body,
        int? listingId = null, int? meetingId = null, CancellationToken ct = default)
    {
        var doc = new NotificationDocument
        {
            UserId = userId, Type = type, Title = title, Body = body,
            ListingId = listingId, MeetingId = meetingId
        };

        try
        {
            _store.AddNotification(doc);
        }
        catch (Exception ex)
        {
            // A notification must never break the action that triggered it.
            _logger.LogError(ex, "Could not save notification for user {UserId}", userId);
        }

        var token = _store.GetDeviceToken(userId);
        if (string.IsNullOrWhiteSpace(token) || !_push.IsEnabled) return;

        var data = new Dictionary<string, string>
        {
            ["type"] = type,
            ["title"] = title,
            ["body"] = body,
            ["notificationId"] = doc.Id,
            ["listingId"] = listingId?.ToString() ?? "",
            ["meetingId"] = meetingId?.ToString() ?? ""
        };

        var result = await _push.SendAsync(token, data, ct);
        if (result == PushResult.InvalidToken) _store.RemoveDeviceTokenValue(token);
    }
}
