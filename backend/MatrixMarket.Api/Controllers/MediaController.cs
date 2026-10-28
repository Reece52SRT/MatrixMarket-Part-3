using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace MatrixMarket.Api.Controllers;

// GET /api/v1/media/{name} - streams a listing photo out of blob storage.
[ApiController]
[Route("api/v1/media")]
public class MediaController : ControllerBase
{
    private readonly IBlobStorageService _blobs;

    public MediaController(IBlobStorageService blobs) => _blobs = blobs;

    [HttpGet("{name}")]
    [ResponseCache(Duration = 86400)]
    public async Task<IActionResult> Get(string name, CancellationToken ct)
    {
        if (!BlobNames.IsSafe(name)) return NotFound();
        var file = await _blobs.OpenReadAsync(name, ct);
        return file == null ? NotFound() : File(file.Content, file.ContentType);
    }
}
