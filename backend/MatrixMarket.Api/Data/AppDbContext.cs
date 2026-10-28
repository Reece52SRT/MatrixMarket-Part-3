using Microsoft.EntityFrameworkCore;
using MatrixMarket.Api.Models;

namespace MatrixMarket.Api.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Category> Categories => Set<Category>();
    public DbSet<Listing> Listings => Set<Listing>();
    public DbSet<Offer> Offers => Set<Offer>();
    public DbSet<TradeMeeting> TradeMeetings => Set<TradeMeeting>();
    public DbSet<TrustBadge> TrustBadges => Set<TrustBadge>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // Explicit primary keys: EF Core's default convention looks for "Id" or
        // "<ClassName>Id" (e.g. TradeMeetingId), but these entities use a shorter,
        // more readable property name (MeetingId, BadgeId), so we declare the keys
        // explicitly rather than relying on convention.
        modelBuilder.Entity<User>().HasKey(u => u.UserId);
        modelBuilder.Entity<Category>().HasKey(c => c.CategoryId);
        modelBuilder.Entity<Listing>().HasKey(l => l.ListingId);
        modelBuilder.Entity<Offer>().HasKey(o => o.OfferId);
        modelBuilder.Entity<TradeMeeting>().HasKey(t => t.MeetingId);
        modelBuilder.Entity<TrustBadge>().HasKey(b => b.BadgeId);

        modelBuilder.Entity<User>().HasIndex(u => u.StudentEmail).IsUnique();

        modelBuilder.Entity<Listing>()
            .HasOne(l => l.Seller)
            .WithMany(u => u.Listings)
            .HasForeignKey(l => l.SellerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Listing>()
            .HasOne(l => l.Category)
            .WithMany()
            .HasForeignKey(l => l.CategoryId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Offer>()
            .HasOne(o => o.Listing)
            .WithMany()
            .HasForeignKey(o => o.ListingId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Offer>()
            .HasOne(o => o.Buyer)
            .WithMany()
            .HasForeignKey(o => o.BuyerId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<TradeMeeting>()
            .HasOne(t => t.Offer)
            .WithMany()
            .HasForeignKey(t => t.OfferId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<TrustBadge>()
            .HasOne(b => b.User)
            .WithMany(u => u.Badges)
            .HasForeignKey(b => b.UserId)
            .OnDelete(DeleteBehavior.Cascade);

        // Seed academic categories so the app has data to show immediately.
        modelBuilder.Entity<Category>().HasData(
            new Category { CategoryId = 1, CategoryName = "Textbooks", Description = "Course textbooks and study guides" },
            new Category { CategoryId = 2, CategoryName = "Electronics", Description = "Laptops, calculators, tech gear" },
            new Category { CategoryId = 3, CategoryName = "Residence Essentials", Description = "Furniture and dorm items" },
            new Category { CategoryId = 4, CategoryName = "Skills & Tutoring", Description = "Peer tutoring and skill exchange" },
            new Category { CategoryId = 5, CategoryName = "Stationery", Description = "Stationery and study supplies" }
        );
    }
}