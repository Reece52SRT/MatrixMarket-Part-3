namespace MatrixMarket.Api.Services;

// Validates book ISBNs (lecturer feedback: "any EAN-13 barcode is accepted as an
// ISBN, including non-book products"). A book's EAN-13 barcode must start with
// the "Bookland" prefix 978 or 979 AND have a correct check digit.
public static class IsbnValidator
{
    // Strips spaces/hyphens. Keeps a trailing X for ISBN-10.
    public static string Normalize(string? input) =>
        new string((input ?? string.Empty).Where(c => char.IsDigit(c) || c == 'X' || c == 'x').ToArray()).ToUpperInvariant();

    public static bool IsValidIsbn13(string isbn)
    {
        if (isbn.Length != 13 || !isbn.All(char.IsDigit)) return false;
        if (!isbn.StartsWith("978") && !isbn.StartsWith("979")) return false;

        var sum = 0;
        for (var i = 0; i < 12; i++)
        {
            var digit = isbn[i] - '0';
            sum += i % 2 == 0 ? digit : digit * 3;
        }
        var check = (10 - sum % 10) % 10;
        return check == isbn[12] - '0';
    }

    public static bool IsValidIsbn10(string isbn)
    {
        if (isbn.Length != 10) return false;
        var sum = 0;
        for (var i = 0; i < 10; i++)
        {
            int value;
            if (char.IsDigit(isbn[i])) value = isbn[i] - '0';
            else if (i == 9 && isbn[i] == 'X') value = 10;
            else return false;
            sum += (10 - i) * value;
        }
        return sum % 11 == 0;
    }

    public static bool IsValid(string? input)
    {
        var isbn = Normalize(input);
        return isbn.Length switch
        {
            13 => IsValidIsbn13(isbn),
            10 => IsValidIsbn10(isbn),
            _ => false
        };
    }
}
