using MatrixMarket.Api.Data;
using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Extensions;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

[ApiController]
[Route("api/v1/users")]
[Authorize]
public class UsersController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly KarmaService _karma;
    private readonly INoSqlStore _noSql;

    public UsersController(AppDbContext db, KarmaService karma, INoSqlStore noSql)
    {
        _db = db;
        _karma = karma;
        _noSql = noSql;
    }

    // GET /api/v1/users/{userId}/profile - Gamified Student Score (User Defined Feature 3).
    [HttpGet("{userId:int}/profile")]
    public async Task<ActionResult<UserProfileResponse>> GetProfile(int userId)
    {
        var user = await _db.Users
            .Include(u => u.Badges)
            .Include(u => u.Listings)
            .FirstOrDefaultAsync(u => u.UserId == userId);

        if (user == null) return NotFound(new ApiMessage("User not found."));

        return Ok(new UserProfileResponse
        {
            UserId = user.UserId,
            FullName = user.FullName,
            StudentEmail = user.StudentEmail,
            Campus = user.Campus,
            KarmaPoints = user.KarmaPoints,
            TrustTier = KarmaService.GetTrustTier(user.KarmaPoints),
            Badges = user.Badges.Select(b => b.BadgeType).ToList(),
            ListingsCount = user.Listings.Count(l => l.Status == "Active"),
            MemberSince = user.CreatedAt,
            TradesCompleted = await _karma.CountCompletedSalesAsync(userId)
        });
    }

    // PUT /api/v1/users/{userId}/settings - returns the saved values so the app can
    // refresh its cached session (lecturer feedback on the Settings feature).
    [HttpPut("{userId:int}/settings")]
    public async Task<ActionResult<UpdateSettingsResponse>> UpdateSettings(int userId, UpdateSettingsRequest request)
    {
        if (User.GetUserId() != userId)
            return StatusCode(StatusCodes.Status403Forbidden, new ApiMessage("You can only change your own settings."));

        var user = await _db.Users.FindAsync(userId);
        if (user == null) return NotFound(new ApiMessage("User not found."));

        if (request.FullName != null)
        {
            var name = request.FullName.Trim();
            if (name.Length < 2 || name.Length > 80) return BadRequest(new ApiMessage("Name must be 2-80 characters."));
            user.FullName = name;
        }
        if (request.Campus != null)
        {
            var campus = request.Campus.Trim();
            if (campus.Length > 80) return BadRequest(new ApiMessage("Campus must be 80 characters or fewer."));
            user.Campus = campus.Length == 0 ? null : campus;
        }

        await _db.SaveChangesAsync();
        return Ok(new UpdateSettingsResponse { UserId = user.UserId, FullName = user.FullName, Campus = user.Campus });
    }

    // PUT /api/v1/users/me/device-token - registers this phone for push notifications.
    [HttpPut("me/device-token")]
    public IActionResult SaveDeviceToken(DeviceTokenRequest request)
    {
        var token = (request.Token ?? string.Empty).Trim();
        if (token.Length < 20 || token.Length > 4096) return BadRequest(new ApiMessage("Invalid device token."));
        _noSql.SaveDeviceToken(User.GetUserId(), token);
        return NoContent();
    }

    // DELETE /api/v1/users/me/device-token - stop push notifications (logout / setting off).
    [HttpDelete("me/device-token")]
    public IActionResult RemoveDeviceToken()
    {
        _noSql.RemoveDeviceToken(User.GetUserId());
        return NoContent();
    }
}
