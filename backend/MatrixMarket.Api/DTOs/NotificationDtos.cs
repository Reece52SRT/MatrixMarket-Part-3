namespace MatrixMarket.Api.DTOs;

public class NotificationResponse
{
    public string Id { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public int? ListingId { get; set; }
    public int? MeetingId { get; set; }
    public bool IsRead { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class UnreadCountResponse
{
    public int Unread { get; set; }
}
