using MatrixMarket.Api.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MatrixMarket.Api.Controllers;

[ApiController]
[Route("api/v1/categories")]
public class CategoriesController : ControllerBase
{
    private readonly AppDbContext _db;
    public CategoriesController(AppDbContext db) => _db = db;

    // GET /api/v1/categories - populates the category filter chips.
    [HttpGet]
    public async Task<IActionResult> GetAll() =>
        Ok(await _db.Categories.OrderBy(c => c.CategoryId).ToListAsync());
}
