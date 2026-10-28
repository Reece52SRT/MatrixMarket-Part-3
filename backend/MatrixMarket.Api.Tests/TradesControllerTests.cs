using MatrixMarket.Api.Controllers;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace MatrixMarket.Api.Tests;

// Covers the lecturer feedback on User Defined Features 2 and 3.
public class TradesControllerTests
{
    private static TradesController NewController(TestDatabase t, FakeNotificationService notifications) =>
        new(t.Db, new KarmaService(t.Db), notifications, TestData.NewNoSqlStore(), NullLogger<TradesController>.Instance);

    [Fact]
    public async Task Confirm_ByUserOutsideTheTrade_IsForbidden()
    {
        using var t = new TestDatabase();
        var trade = TestData.SeedTrade(t.Db);
        var controller = NewController(t, new FakeNotificationService());
        TestData.LoginAs(controller, trade.Stranger.UserId);

        var result = await controller.Confirm(trade.Meeting.MeetingId, new ConfirmTradeRequest { ConfirmationCode = "123456" }, default);

        Assert.Equal(403, TestData.StatusOf(result));
    }

    [Fact]
    public async Task Confirm_ByBuyer_IsForbidden_OnlySellerConfirms()
    {
        using var t = new TestDatabase();
        var trade = TestData.SeedTrade(t.Db);
        var controller = NewController(t, new FakeNotificationService());
        TestData.LoginAs(controller, trade.Buyer.UserId);

        var result = await controller.Confirm(trade.Meeting.MeetingId, new ConfirmTradeRequest { ConfirmationCode = "123456" }, default);

        Assert.Equal(403, TestData.StatusOf(result));
    }

    [Fact]
    public async Task Confirm_WithWrongCode_IsRejected()
    {
        using var t = new TestDatabase();
        var trade = TestData.SeedTrade(t.Db);
        var controller = NewController(t, new FakeNotificationService());
        TestData.LoginAs(controller, trade.Seller.UserId);

        var result = await controller.Confirm(trade.Meeting.MeetingId, new ConfirmTradeRequest { ConfirmationCode = "000000" }, default);

        Assert.Equal(400, TestData.StatusOf(result));
    }

    [Fact]
    public async Task Confirm_Twice_AwardsKarmaOnlyOnce()
    {
        using var t = new TestDatabase();
        var trade = TestData.SeedTrade(t.Db);
        var controller = NewController(t, new FakeNotificationService());
        TestData.LoginAs(controller, trade.Seller.UserId);
        var request = new ConfirmTradeRequest { ConfirmationCode = "123456" };

        var first = await controller.Confirm(trade.Meeting.MeetingId, request, default);
        var second = await controller.Confirm(trade.Meeting.MeetingId, request, default);

        Assert.Equal(200, TestData.StatusOf(first));
        Assert.Equal(409, TestData.StatusOf(second));

        var seller = await t.Db.Users.AsNoTracking().FirstAsync(u => u.UserId == trade.Seller.UserId);
        var buyer = await t.Db.Users.AsNoTracking().FirstAsync(u => u.UserId == trade.Buyer.UserId);
        Assert.Equal(KarmaService.SellerTradePoints, seller.KarmaPoints);
        Assert.Equal(KarmaService.BuyerTradePoints, buyer.KarmaPoints);
    }

    [Fact]
    public async Task Schedule_ByUserOutsideTheTrade_IsForbidden()
    {
        using var t = new TestDatabase();
        var seller = TestData.AddUser(t.Db, "Seller");
        var buyer = TestData.AddUser(t.Db, "Buyer");
        var stranger = TestData.AddUser(t.Db, "Stranger");
        var listing = new MatrixMarket.Api.Models.Listing { SellerId = seller.UserId, CategoryId = 1, Title = "Lamp", Description = "Desk lamp", Price = 50 };
        t.Db.Listings.Add(listing);
        await t.Db.SaveChangesAsync();
        var offer = new MatrixMarket.Api.Models.Offer { ListingId = listing.ListingId, BuyerId = buyer.UserId, OfferedPrice = 50 };
        t.Db.Offers.Add(offer);
        await t.Db.SaveChangesAsync();

        var controller = NewController(t, new FakeNotificationService());
        TestData.LoginAs(controller, stranger.UserId);

        var result = await controller.Schedule(new ScheduleTradeRequest
        {
            OfferId = offer.OfferId, CampusSpot = "Library Foyer", MeetingTime = DateTime.UtcNow.AddDays(2)
        }, default);

        Assert.Equal(403, TestData.StatusOf(result.Result!));
    }

    [Fact]
    public async Task Schedule_ByBuyer_CreatesMeeting_AndNotifiesSeller()
    {
        using var t = new TestDatabase();
        var seller = TestData.AddUser(t.Db, "Seller");
        var buyer = TestData.AddUser(t.Db, "Buyer");
        var listing = new MatrixMarket.Api.Models.Listing { SellerId = seller.UserId, CategoryId = 1, Title = "Lamp", Description = "Desk lamp", Price = 50 };
        t.Db.Listings.Add(listing);
        await t.Db.SaveChangesAsync();
        var offer = new MatrixMarket.Api.Models.Offer { ListingId = listing.ListingId, BuyerId = buyer.UserId, OfferedPrice = 50 };
        t.Db.Offers.Add(offer);
        await t.Db.SaveChangesAsync();

        var notifications = new FakeNotificationService();
        var controller = NewController(t, notifications);
        TestData.LoginAs(controller, buyer.UserId);

        var result = await controller.Schedule(new ScheduleTradeRequest
        {
            OfferId = offer.OfferId, CampusSpot = "Library Foyer", MeetingTime = DateTime.UtcNow.AddDays(2)
        }, default);

        Assert.Equal(200, TestData.StatusOf(result.Result!));
        Assert.Contains(notifications.Sent, n => n.UserId == seller.UserId && n.Type == "meeting");
        Assert.Equal(1, await t.Db.TradeMeetings.CountAsync());
    }
}
