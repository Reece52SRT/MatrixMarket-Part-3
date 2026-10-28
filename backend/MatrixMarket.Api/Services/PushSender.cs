using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;

namespace MatrixMarket.Api.Services;

public enum PushResult { Sent, InvalidToken, Failed, Disabled }

// Sends a real-time push notification to one phone.
public interface IPushSender
{
    bool IsEnabled { get; }
    Task<PushResult> SendAsync(string deviceToken, IReadOnlyDictionary<string, string> data, CancellationToken ct = default);
}

// Firebase Cloud Messaging (HTTP v1) via the Firebase Admin SDK.
// We send DATA messages so the Android app always builds the notification itself
// (this lets the user's "Notifications" setting be respected even in the background).
public class FcmPushSender : IPushSender
{
    private readonly FirebaseMessaging _messaging;
    private readonly ILogger<FcmPushSender> _logger;

    private FcmPushSender(FirebaseMessaging messaging, ILogger<FcmPushSender> logger)
    {
        _messaging = messaging;
        _logger = logger;
    }

    public bool IsEnabled => true;

    // Uses the Firebase service-account JSON from configuration
    // (Firebase:ServiceAccountJson or the FIREBASE_SERVICE_ACCOUNT_JSON env var).
    // If it is missing, pushes are switched off but everything else keeps working.
    public static IPushSender CreateOrNoOp(IConfiguration config, ILoggerFactory loggerFactory)
    {
        var json = config["Firebase:ServiceAccountJson"];
        if (string.IsNullOrWhiteSpace(json))
            json = Environment.GetEnvironmentVariable("FIREBASE_SERVICE_ACCOUNT_JSON");

        var logger = loggerFactory.CreateLogger<FcmPushSender>();
        if (string.IsNullOrWhiteSpace(json))
        {
            logger.LogWarning("Firebase service account not configured - push notifications disabled (inbox still works).");
            return new NoOpPushSender();
        }

        try
        {
            var app = FirebaseApp.DefaultInstance ?? FirebaseApp.Create(new AppOptions
            {
#pragma warning disable CS0618 // FromJson is fine for a service-account key supplied via configuration
                Credential = GoogleCredential.FromJson(json),
#pragma warning restore CS0618
                ProjectId = config["Firebase:ProjectId"]
            });
            logger.LogInformation("Firebase Cloud Messaging enabled for project {Project}", app.Options.ProjectId);
            return new FcmPushSender(FirebaseMessaging.GetMessaging(app), logger);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Could not initialise Firebase Admin - push notifications disabled.");
            return new NoOpPushSender();
        }
    }

    public async Task<PushResult> SendAsync(string deviceToken, IReadOnlyDictionary<string, string> data, CancellationToken ct = default)
    {
        try
        {
            var message = new Message
            {
                Token = deviceToken,
                Data = data,
                Android = new AndroidConfig { Priority = Priority.High, TimeToLive = TimeSpan.FromDays(1) }
            };
            var id = await _messaging.SendAsync(message, ct);
            _logger.LogInformation("FCM message sent: {MessageId}", id);
            return PushResult.Sent;
        }
        catch (FirebaseMessagingException ex) when (
            ex.MessagingErrorCode == MessagingErrorCode.Unregistered ||
            ex.MessagingErrorCode == MessagingErrorCode.InvalidArgument)
        {
            _logger.LogWarning("FCM token rejected ({Code}) - removing it", ex.MessagingErrorCode);
            return PushResult.InvalidToken;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "FCM send failed");
            return PushResult.Failed;
        }
    }
}

public class NoOpPushSender : IPushSender
{
    public bool IsEnabled => false;

    public Task<PushResult> SendAsync(string deviceToken, IReadOnlyDictionary<string, string> data, CancellationToken ct = default) =>
        Task.FromResult(PushResult.Disabled);
}
