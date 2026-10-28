using MatrixMarket.Api.Models;

namespace MatrixMarket.Api.Data;

// Populates the database with realistic demo data on startup so the app has
// something to show immediately (listings feed, offers, a scheduled trade
// meeting) instead of an empty state. Safe to run every startup - it checks
// what already exists and only adds what's missing, so it never duplicates
// data or touches real accounts you've created by registering in the app.
public static class DbSeeder
{
    // All demo accounts share this password so you can log into any of them
    // from the app while testing.
    public const string DemoPassword = "Password123!";

    public static void Seed(AppDbContext db)
    {
        // Users are saved immediately so their UserIds exist before listings/offers
        // reference them.
        var demoUsers = SeedUsers(db);

        var listings = SeedListings(db, demoUsers);
        db.SaveChanges(); // assigns ListingIds before offers reference them

        SeedOffersAndMeeting(db, demoUsers, listings);
        db.SaveChanges();
    }

    private static List<User> SeedUsers(AppDbContext db)
    {
        var seedUserSpecs = new (string Email, string Name, string Campus, int Karma)[]
        {
            ("thabo.mokoena@myemeris.edu.za", "Thabo Mokoena", "Emeris Westville", 42),
            ("amahle.dlamini@myemeris.edu.za", "Amahle Dlamini", "Emeris Westville", 18),
            ("sam.pillay@myemeris.edu.za", "Sam Pillay", "Emeris Howard College", 65),
            ("zanele.khumalo@myemeris.edu.za", "Zanele Khumalo", "Emeris Howard College", 7),
            ("ryan.govender@myemeris.edu.za", "Ryan Govender", "Emeris Westville", 30),
        };

        var existingEmails = db.Users.Select(u => u.StudentEmail).ToHashSet();

        foreach (var spec in seedUserSpecs)
        {
            if (!existingEmails.Contains(spec.Email))
            {
                db.Users.Add(new User
                {
                    StudentEmail = spec.Email,
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword(DemoPassword),
                    FullName = spec.Name,
                    Campus = spec.Campus,
                    KarmaPoints = spec.Karma,
                    SsoProvider = "Email"
                });
            }
        }

        db.SaveChanges(); // persist so UserIds are assigned

        var seedEmails = seedUserSpecs.Select(s => s.Email).ToHashSet();
        return db.Users.Where(u => seedEmails.Contains(u.StudentEmail)).ToList();
    }

    private static List<Listing> SeedListings(AppDbContext db, List<User> demoUsers)
    {
        if (db.Listings.Any()) return db.Listings.ToList();

        // CategoryIds match the seed data in AppDbContext.OnModelCreating:
        // 1 Textbooks, 2 Electronics, 3 Residence Essentials, 4 Skills & Tutoring, 5 Stationery
        User Seller(int i) => demoUsers[i % demoUsers.Count];

        var listings = new List<Listing>
        {
            new() { Seller = Seller(0), CategoryId = 1, Title = "Calculus: Early Transcendentals (8th Ed)",
                Description = "Stewart's Calculus, used one semester. Minor highlighting in chapters 1-4, otherwise clean.",
                Price = 450, ListingType = "Sell", Isbn = "9781285741550" },

            new() { Seller = Seller(1), CategoryId = 1, Title = "Organic Chemistry - Klein 3rd Edition",
                Description = "Great condition, comes with the solutions manual. Willing to trade for a Physics textbook.",
                Price = 600, ListingType = "Trade", Isbn = "9781119320296" },

            new() { Seller = Seller(2), CategoryId = 1, Title = "Introduction to Algorithms (CLRS)",
                Description = "The classic CS textbook. Lending out for the semester, just need it back by exam time.",
                Price = 0, ListingType = "Lend", Isbn = "9780262046305" },

            new() { Seller = Seller(3), CategoryId = 1, Title = "Campbell Biology (12th Ed)",
                Description = "Barely used, no markings. Selling because I switched majors.",
                Price = 550, ListingType = "Sell", Isbn = "9780135188743" },

            new() { Seller = Seller(4), CategoryId = 2, Title = "Casio FX-991 Scientific Calculator",
                Description = "Works perfectly, screen protector still on. Ideal for engineering/stats modules.",
                Price = 180, ListingType = "Sell" },

            new() { Seller = Seller(0), CategoryId = 2, Title = "Dell Inspiron 15\" Laptop (i5, 8GB RAM)",
                Description = "Good daily driver for lectures and assignments. Battery lasts ~5 hours. Charger included.",
                Price = 4200, ListingType = "Sell" },

            new() { Seller = Seller(1), CategoryId = 2, Title = "USB-C Hub / Dock",
                Description = "7-in-1 hub, HDMI + 3x USB + SD card reader. Only used a few times.",
                Price = 250, ListingType = "Trade" },

            new() { Seller = Seller(2), CategoryId = 3, Title = "Mini Bar Fridge (Res Room Size)",
                Description = "Perfect for res. Clean, no smell, works great. Moving out and can't take it home.",
                Price = 700, ListingType = "Sell" },

            new() { Seller = Seller(3), CategoryId = 3, Title = "Desk Lamp with USB Charging Port",
                Description = "Adjustable brightness, barely used. Lending until end of exams.",
                Price = 0, ListingType = "Lend" },

            new() { Seller = Seller(4), CategoryId = 4, Title = "1-on-1 Stats Tutoring (STAT101/201)",
                Description = "3rd year Stats student, happy to help with assignments or exam prep. Flexible times.",
                Price = 150, ListingType = "Sell" },

            new() { Seller = Seller(0), CategoryId = 4, Title = "Python & R Coding Help",
                Description = "Can help debug assignments or explain concepts for intro programming/data science modules.",
                Price = 100, ListingType = "Trade" },

            new() { Seller = Seller(1), CategoryId = 5, Title = "Scientific Calculator + Geometry Set Bundle",
                Description = "Selling as a bundle - both in good condition, geometry set barely used.",
                Price = 120, ListingType = "Sell" },

            new() { Seller = Seller(2), CategoryId = 5, Title = "A4 Refill Pad Pack (x5) + Folders",
                Description = "Bought too many at the start of term. Never opened.",
                Price = 90, ListingType = "Sell" },
        };

        db.Listings.AddRange(listings);
        return listings;
    }

    private static void SeedOffersAndMeeting(AppDbContext db, List<User> demoUsers, List<Listing> listings)
    {
        if (db.Offers.Any() || listings.Count < 5) return;

        User Buyer(int i) => demoUsers[i % demoUsers.Count];

        // A pending offer on the Organic Chemistry trade listing.
        var pendingOffer = new Offer
        {
            Listing = listings[1],
            Buyer = Buyer(3),
            OfferedPrice = 550,
            Status = "Pending"
        };

        // An accepted offer on the calculator, with a scheduled trade meeting -
        // so the Trade Meeting screen has something to show out of the box.
        var acceptedOffer = new Offer
        {
            Listing = listings[4],
            Buyer = Buyer(2),
            OfferedPrice = 180,
            Status = "Accepted"
        };

        db.Offers.AddRange(pendingOffer, acceptedOffer);
        db.SaveChanges(); // so acceptedOffer.OfferId is populated before the meeting references it

        db.TradeMeetings.Add(new TradeMeeting
        {
            Offer = acceptedOffer,
            CampusSpot = "Student Center North",
            MeetingTime = DateTime.UtcNow.AddDays(1).Date.AddHours(14),
            ConfirmationCode = "482913",
            IsConfirmed = false
        });
    }
}