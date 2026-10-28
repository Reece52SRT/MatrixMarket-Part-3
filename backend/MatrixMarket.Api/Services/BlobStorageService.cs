using Azure.Storage.Blobs;
using Azure.Storage.Blobs.Models;

namespace MatrixMarket.Api.Services;

// Stores listing photos (User Defined Feature 4). Images are always served back
// through GET /api/v1/media/{name}, so it does not matter whether the storage
// account allows public access.
public interface IBlobStorageService
{
    Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default);
    Task<BlobFile?> OpenReadAsync(string blobName, CancellationToken ct = default);
    Task DeleteAsync(string blobName, CancellationToken ct = default);
}

public record BlobFile(Stream Content, string ContentType);

public static class BlobNames
{
    // Only names we generated ourselves are accepted (prevents path traversal).
    public static bool IsSafe(string name) =>
        !string.IsNullOrWhiteSpace(name) && name.Length <= 80 &&
        name.All(c => char.IsLetterOrDigit(c) || c == '-' || c == '_' || c == '.') &&
        !name.Contains("..");

    public static string NewName(string extension) => $"{Guid.NewGuid():N}{extension}";

    public static string ContentTypeFor(string name) => Path.GetExtension(name).ToLowerInvariant() switch
    {
        ".png" => "image/png",
        ".webp" => "image/webp",
        _ => "image/jpeg"
    };
}

// Azure Blob Storage implementation (used when a connection string is configured).
public class AzureBlobStorageService : IBlobStorageService
{
    private readonly BlobContainerClient _container;
    private readonly ILogger<AzureBlobStorageService> _logger;
    private bool _containerReady;

    public AzureBlobStorageService(string connectionString, string containerName, ILogger<AzureBlobStorageService> logger)
    {
        _container = new BlobContainerClient(connectionString, containerName);
        _logger = logger;
    }

    private async Task EnsureContainerAsync(CancellationToken ct)
    {
        if (_containerReady) return;
        await _container.CreateIfNotExistsAsync(PublicAccessType.None, cancellationToken: ct);
        _containerReady = true;
    }

    public async Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default)
    {
        await EnsureContainerAsync(ct);
        var name = BlobNames.NewName(extension);
        var blob = _container.GetBlobClient(name);
        await blob.UploadAsync(content, new BlobUploadOptions { HttpHeaders = new BlobHttpHeaders { ContentType = contentType } }, ct);
        _logger.LogInformation("Uploaded blob {Name} to Azure container {Container}", name, _container.Name);
        return name;
    }

    public async Task<BlobFile?> OpenReadAsync(string blobName, CancellationToken ct = default)
    {
        await EnsureContainerAsync(ct);
        var blob = _container.GetBlobClient(blobName);
        if (!await blob.ExistsAsync(ct)) return null;
        var download = await blob.DownloadStreamingAsync(cancellationToken: ct);
        var type = download.Value.Details.ContentType ?? BlobNames.ContentTypeFor(blobName);
        return new BlobFile(download.Value.Content, type);
    }

    public async Task DeleteAsync(string blobName, CancellationToken ct = default)
    {
        await EnsureContainerAsync(ct);
        await _container.DeleteBlobIfExistsAsync(blobName, cancellationToken: ct);
    }
}

// Local-disk fallback so the API still works when no Azure storage is configured.
public class LocalBlobStorageService : IBlobStorageService
{
    private readonly string _directory;
    private readonly ILogger<LocalBlobStorageService> _logger;

    public LocalBlobStorageService(string directory, ILogger<LocalBlobStorageService> logger)
    {
        _directory = directory;
        _logger = logger;
        Directory.CreateDirectory(_directory);
    }

    public async Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default)
    {
        var name = BlobNames.NewName(extension);
        await using var file = File.Create(Path.Combine(_directory, name));
        await content.CopyToAsync(file, ct);
        _logger.LogInformation("Stored image {Name} on local disk", name);
        return name;
    }

    public Task<BlobFile?> OpenReadAsync(string blobName, CancellationToken ct = default)
    {
        var path = Path.Combine(_directory, blobName);
        if (!File.Exists(path)) return Task.FromResult<BlobFile?>(null);
        Stream stream = File.OpenRead(path);
        return Task.FromResult<BlobFile?>(new BlobFile(stream, BlobNames.ContentTypeFor(blobName)));
    }

    public Task DeleteAsync(string blobName, CancellationToken ct = default)
    {
        var path = Path.Combine(_directory, blobName);
        if (File.Exists(path)) File.Delete(path);
        return Task.CompletedTask;
    }
}
