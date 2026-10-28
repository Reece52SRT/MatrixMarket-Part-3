using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace MatrixMarket.Api.Extensions;

// Reads the logged-in user's id out of the JWT. .NET may map the "sub" claim to
// ClaimTypes.NameIdentifier, so both names are checked.
public static class ClaimsPrincipalExtensions
{
    public static int GetUserId(this ClaimsPrincipal user)
    {
        var value = user.FindFirstValue(ClaimTypes.NameIdentifier)
                    ?? user.FindFirstValue(JwtRegisteredClaimNames.Sub);
        return int.TryParse(value, out var id) ? id : 0;
    }
}
