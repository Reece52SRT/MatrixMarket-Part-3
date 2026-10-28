namespace MatrixMarket.Api.DTOs;

public class CreateOfferRequest
{
    public int ListingId { get; set; }
    public decimal OfferedPrice { get; set; }
}

public class OfferResponse
{
    public int OfferId { get; set; }
    public string Status { get; set; } = string.Empty;
}

public class ScheduleTradeRequest
{
    public int OfferId { get; set; }
    public string CampusSpot { get; set; } = string.Empty;
    // Campus wall-clock time (South Africa), e.g. 2026-10-23T14:00:00
    public DateTime MeetingTime { get; set; }
}

public class TradeMeetingResponse
{
    public int MeetingId { get; set; }
    public int OfferId { get; set; }
    public string CampusSpot { get; set; } = string.Empty;
    public DateTime MeetingTime { get; set; }
    // Only returned to the BUYER - the seller has to be shown it in person.
    public string ConfirmationCode { get; set; } = string.Empty;
    public bool IsConfirmed { get; set; }
    public string ListingTitle { get; set; } = string.Empty;
}

public class ConfirmTradeRequest
{
    public string ConfirmationCode { get; set; } = string.Empty;
}

// Returned by GET /api/v1/trades/mine so both sides of a trade can see it.
public class MyTradeMeetingResponse
{
    public int MeetingId { get; set; }
    public int OfferId { get; set; }
    public string CampusSpot { get; set; } = string.Empty;
    public DateTime MeetingTime { get; set; }
    public string ConfirmationCode { get; set; } = string.Empty; // empty for the seller
    public bool IsConfirmed { get; set; }
    public bool CanConfirm { get; set; } // true only for the seller of an unconfirmed meeting
    public string ListingTitle { get; set; } = string.Empty;
    public decimal Price { get; set; }
    public string Role { get; set; } = string.Empty; // "Buyer" or "Seller"
    public string OtherPartyName { get; set; } = string.Empty;
}
