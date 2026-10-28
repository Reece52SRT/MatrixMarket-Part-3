using MatrixMarket.Api.DTOs;
using MatrixMarket.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MatrixMarket.Api.Controllers;

// GET /api/v1/books/{isbn} - validates an ISBN and returns the textbook's details
// so a scanned book can pre-fill the listing (User Defined Feature 1).
[ApiController]
[Route("api/v1/books")]
[Authorize]
public class BooksController : ControllerBase
{
    private readonly IBookLookupService _lookup;

    public BooksController(IBookLookupService lookup) => _lookup = lookup;

    [HttpGet("{isbn}")]
    public async Task<ActionResult<BookInfoResponse>> Get(string isbn, CancellationToken ct)
    {
        if (!IsbnValidator.IsValid(isbn))
            return BadRequest(new ApiMessage("That barcode is not a valid book ISBN."));

        var normalized = IsbnValidator.Normalize(isbn);
        var info = await _lookup.LookupAsync(normalized, ct);
        return info == null
            ? NotFound(new ApiMessage("No book details found for this ISBN - please type the title yourself."))
            : Ok(info);
    }
}
