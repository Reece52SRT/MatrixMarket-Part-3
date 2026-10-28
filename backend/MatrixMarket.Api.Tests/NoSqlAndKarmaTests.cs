using MatrixMarket.Api.Models;
using MatrixMarket.Api.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace MatrixMarket.Api.Tests;

public class NoSqlAndKarmaTests
{
    [Theory]
    [InlineData(0, "New Trader")]
    [InlineData(40, "Rising Trader")]
    [InlineData(100, "Trusted Trader")]
    [InlineData(250, "Campus Legend")]
    public void TrustTier_MatchesThresholds(int points, string expected) =>
        Assert.Equal(expected, KarmaService.GetTrustTier(points));

    [Fact]
    public void Watchlist_AddListRemove_RoundTrips()
    {
        var store = TestData.NewNoSqlStore();
        store.AddToWatchlist(1, 10);
        store.AddToWatchlist(1, 11);
        store.AddToWatchlist(1, 10); // saving twice must not duplicate

        Assert.Equal(2, store.GetWatchlistListingIds(1).Count);
        Assert.Contains(1, store.GetWatchers(10));

        Assert.True(store.RemoveFromWatchlist(1, 10));
        Assert.Equal(new List<int> { 11 }, store.GetWatchlistListingIds(1));
    }

    [Fact]
    public async Task NotificationService_SavesToInbox_EvenWhenPushIsDisabled()
    {
        var store = TestData.NewNoSqlStore();
        var service = new NotificationService(store, new NoOpPushSender(), NullLogger<NotificationService>.Instance);

        await service.NotifyAsync(5, "offer", "New offer received", "R100 for your calculator");

        var inbox = store.GetNotifications(5);
        Assert.Single(inbox);
        Assert.Equal(1, store.CountUnread(5));
        Assert.True(store.MarkRead(5, inbox[0].Id));
        Assert.Equal(0, store.CountUnread(5));
    }

    [Fact]
    public void DeviceToken_MovesToNewestAccountOnSamePhone()
    {
        var store = TestData.NewNoSqlStore();
        const string token = "fcm-token-abcdefghijklmnopqrstuvwxyz";
        store.SaveDeviceToken(1, token);
        store.SaveDeviceToken(2, token);

        Assert.Null(store.GetDeviceToken(1));
        Assert.Equal(token, store.GetDeviceToken(2));
    }
}
