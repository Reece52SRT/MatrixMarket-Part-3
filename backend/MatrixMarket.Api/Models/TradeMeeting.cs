namespace MatrixMarket.Api.Models;

public class TradeMeeting
{
    public int MeetingId { get; set; }
    public int OfferId { get; set; }
    public Offer? Offer { get; set; }
    public string CampusSpot { get; set; } = string.Empty;
    public DateTime MeetingTime { get; set; }
    public string ConfirmationCode { get; set; } = string.Empty;
    public bool IsConfirmed { get; set; } = false;
}