using MatrixMarket.Api.Data;
using MatrixMarket.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Services;

// Encapsulates the Student Score / Trust Badge gamification logic (User Defined Feature 3).
// Points are only ever awarded from TradesController.Confirm, which uses an atomic
// "confirm only if not yet confirmed" update - so each trade awards points exactly once.
public class KarmaService
{
    public const int SellerTradePoints = 15;
    public const int BuyerTradePoints = 5;

    private readonly AppDbContext _db;

    public KarmaService(AppDbContext db)
    {
        _db = db;
    }

    public static string GetTrustTier(int karmaPoints) => karmaPoints switch
    {
        >= 200 => "Campus Legend",
        >= 100 => "Trusted Trader",
        >= 40 => "Rising Trader",
        _ => "New Trader"
    };

    // Called exactly once per successfully confirmed trade.
    public async Task AwardTradeCompletionAsync(int sellerId, int buyerId)
    {
        var seller = await _db.Users.FindAsync(sellerId);
        if (seller != null)
        {
            seller.KarmaPoints += SellerTradePoints;
            await EvaluateBadgesAsync(seller);
        }

        if (buyerId != sellerId)
        {
            var buyer = await _db.Users.FindAsync(buyerId);
            if (buyer != null) buyer.KarmaPoints += BuyerTradePoints;
        }

        await _db.SaveChangesAsync();
    }

    public async Task<int> CountCompletedSalesAsync(int userId) =>
        await _db.TradeMeetings
            .Where(t => t.IsConfirmed && t.Offer!.Listing!.SellerId == userId)
            .CountAsync();

    private async Task EvaluateBadgesAsync(User user)
    {
        var existingBadges = await _db.TrustBadges
            .Where(b => b.UserId == user.UserId)
            .Select(b => b.BadgeType)
            .ToListAsync();

        var completedTrades = await CountCompletedSalesAsync(user.UserId);

        if (completedTrades >= 1 && !existingBadges.Contains("First Trade"))
            _db.TrustBadges.Add(new TrustBadge { UserId = user.UserId, BadgeType = "First Trade" });

        if (completedTrades >= 5 && !existingBadges.Contains("Top Seller"))
            _db.TrustBadges.Add(new TrustBadge { UserId = user.UserId, BadgeType = "Top Seller" });

        if (user.KarmaPoints >= 100 && !existingBadges.Contains("Trusted Trader"))
            _db.TrustBadges.Add(new TrustBadge { UserId = user.UserId, BadgeType = "Trusted Trader" });
    }
}
