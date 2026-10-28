using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Extensions;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MatrixMarket.Api.Controllers;

// Reads the user's notification inbox from the NoSQL database.
[ApiController]
[Route("api/v1/notifications")]
[Authorize]
public class NotificationsController : ControllerBase
{
    private readonly INoSqlStore _store;

    public NotificationsController(INoSqlStore store) => _store = store;

    // GET /api/v1/notifications
    [HttpGet]
    public ActionResult<List<NotificationResponse>> GetMine() =>
        Ok(_store.GetNotifications(User.GetUserId()).Select(n => new NotificationResponse
        {
            Id = n.Id, Type = n.Type, Title = n.Title, Body = n.Body,
            ListingId = n.ListingId, MeetingId = n.MeetingId, IsRead = n.IsRead, CreatedAt = n.CreatedAt
        }).ToList());

    // GET /api/v1/notifications/unread-count
    [HttpGet("unread-count")]
    public ActionResult<UnreadCountResponse> UnreadCount() =>
        Ok(new UnreadCountResponse { Unread = _store.CountUnread(User.GetUserId()) });

    // POST /api/v1/notifications/{id}/read
    [HttpPost("{id}/read")]
    public IActionResult MarkRead(string id) =>
        _store.MarkRead(User.GetUserId(), id) ? NoContent() : NotFound(new ApiMessage("Notification not found."));

    // POST /api/v1/notifications/read-all
    [HttpPost("read-all")]
    public IActionResult MarkAllRead()
    {
        _store.MarkAllRead(User.GetUserId());
        return NoContent();
    }
}
