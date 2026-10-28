using System.Text.Json;
using MatrixMarket.Api.DTOs;

namespace MatrixMarket.Api.Services;

// Looks up textbook details for a scanned ISBN so the listing form can be pre-filled
// (User Defined Feature 1). Uses the public Google Books API - no key required.
public interface IBookLookupService
{
    Task<BookInfoResponse?> LookupAsync(string isbn, CancellationToken ct = default);
}

public class GoogleBooksLookupService : IBookLookupService
{
    private readonly HttpClient _http;
    private readonly ILogger<GoogleBooksLookupService> _logger;

    public GoogleBooksLookupService(HttpClient http, ILogger<GoogleBooksLookupService> logger)
    {
        _http = http;
        _logger = logger;
    }

    public async Task<BookInfoResponse?> LookupAsync(string isbn, CancellationToken ct = default)
    {
        try
        {
            using var response = await _http.GetAsync($"https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}", ct);
            if (!response.IsSuccessStatusCode) return null;

            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            using var json = await JsonDocument.ParseAsync(stream, cancellationToken: ct);

            if (!json.RootElement.TryGetProperty("items", out var items) || items.GetArrayLength() == 0) return null;
            var info = items[0].GetProperty("volumeInfo");

            string? Str(string name) => info.TryGetProperty(name, out var v) && v.ValueKind == JsonValueKind.String ? v.GetString() : null;

            var authors = info.TryGetProperty("authors", out var a) && a.ValueKind == JsonValueKind.Array
                ? a.EnumerateArray().Select(x => x.GetString() ?? "").Where(x => x.Length > 0).ToList()
                : new List<string>();

            var title = Str("title");
            if (string.IsNullOrWhiteSpace(title)) return null;
            var subtitle = Str("subtitle");

            return new BookInfoResponse
            {
                Isbn = isbn,
                Title = string.IsNullOrWhiteSpace(subtitle) ? title! : $"{title}: {subtitle}",
                Authors = authors,
                Publisher = Str("publisher"),
                PublishedDate = Str("publishedDate"),
                Description = Str("description")
            };
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Book lookup failed for {Isbn}", isbn);
            return null;
        }
    }
}
