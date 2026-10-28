using System.Security.Claims;
using LiteDB;
using MatrixMarket.Api.Data;
using MatrixMarket.Api.Models;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Tests;

// Real SQLite database living in memory for the duration of one test.
public sealed class TestDatabase : IDisposable
{
    private readonly SqliteConnection _connection;
    public AppDbContext Db { get; }

    public TestDatabase()
    {
        _connection = new SqliteConnection("DataSource=:memory:");
        _connection.Open();
        var options = new DbContextOptionsBuilder<AppDbContext>().UseSqlite(_connection).Options;
        Db = new AppDbContext(options);
        Db.Database.EnsureCreated(); // also seeds the 5 categories
    }

    public void Dispose()
    {
        Db.Dispose();
        _connection.Dispose();
    }
}

// Records notifications instead of sending them.
public class FakeNotificationService : INotificationService
{
    public List<(int UserId, string Type)> Sent { get; } = new();

    public Task NotifyAsync(int userId, string type, string title, string body,
        int? listingId = null, int? meetingId = null, CancellationToken ct = default)
    {
        Sent.Add((userId, type));
        return Task.CompletedTask;
    }
}

public static class TestData
{
    public static LiteDbNoSqlStore NewNoSqlStore() => new(new LiteDatabase(new MemoryStream()));

    public static User AddUser(AppDbContext db, string name)
    {
        var user = new User
        {
            StudentEmail = $"{name.ToLowerInvariant()}@test.ac.za",
            FullName = name,
            PasswordHash = "x",
            SsoProvider = "Email"
        };
        db.Users.Add(user);
        db.SaveChanges();
        return user;
    }

    // Seller lists an item, buyer makes an offer and a meeting is scheduled (code 123456).
    public static (User Seller, User Buyer, User Stranger, Listing Listing, Offer Offer, TradeMeeting Meeting) SeedTrade(AppDbContext db)
    {
        var seller = AddUser(db, "Seller");
        var buyer = AddUser(db, "Buyer");
        var stranger = AddUser(db, "Stranger");

        var listing = new Listing { SellerId = seller.UserId, CategoryId = 1, Title = "Calculus", Description = "Good condition", Price = 300, Status = "Reserved" };
        db.Listings.Add(listing);
        db.SaveChanges();

        var offer = new Offer { ListingId = listing.ListingId, BuyerId = buyer.UserId, OfferedPrice = 300, Status = "Accepted" };
        db.Offers.Add(offer);
        db.SaveChanges();

        var meeting = new TradeMeeting
        {
            OfferId = offer.OfferId, CampusSpot = "Library Foyer",
            MeetingTime = DateTime.UtcNow.AddDays(1), ConfirmationCode = "123456"
        };
        db.TradeMeetings.Add(meeting);
        db.SaveChanges();

        return (seller, buyer, stranger, listing, offer, meeting);
    }

    // Makes a controller act as if the given user is logged in.
    public static void LoginAs(ControllerBase controller, int userId)
    {
        var identity = new ClaimsIdentity(new[] { new Claim(ClaimTypes.NameIdentifier, userId.ToString()) }, "Test");
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(identity) }
        };
    }

    public static int? StatusOf(IActionResult result) => result switch
    {
        ObjectResult o => o.StatusCode,
        StatusCodeResult s => s.StatusCode,
        _ => null
    };
}
