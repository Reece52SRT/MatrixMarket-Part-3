using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace MatrixMarket.Api.Controllers;

// GET /api/v1/health - quick check used to wake the Azure app and in the demo video.
[ApiController]
[Route("api/v1/health")]
public class HealthController : ControllerBase
{
    private readonly IPushSender _push;
    private readonly IBlobStorageService _blobs;

    public HealthController(IPushSender push, IBlobStorageService blobs)
    {
        _push = push;
        _blobs = blobs;
    }

    [HttpGet]
    public IActionResult Get() => Ok(new
    {
        status = "ok",
        timeUtc = DateTime.UtcNow,
        pushNotifications = _push.IsEnabled ? "enabled" : "disabled",
        blobStorage = _blobs is AzureBlobStorageService ? "azure" : "local"
    });
}
