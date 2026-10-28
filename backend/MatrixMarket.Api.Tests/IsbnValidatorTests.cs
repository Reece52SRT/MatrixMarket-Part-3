using MatrixMarket.Api.Services;
using Xunit;

namespace MatrixMarket.Api.Tests;

public class IsbnValidatorTests
{
    [Theory]
    [InlineData("9781285741550")]       // Stewart Calculus
    [InlineData("978-0-262-04630-5")]   // hyphens are allowed
    [InlineData("9780306406157")]
    [InlineData("0306406152")]          // ISBN-10
    [InlineData("080442957X")]          // ISBN-10 with X check digit
    public void ValidIsbns_AreAccepted(string isbn) => Assert.True(IsbnValidator.IsValid(isbn));

    [Theory]
    [InlineData("5000112637922")]  // real grocery EAN-13: correct checksum but not a book
    [InlineData("9781285741551")]  // wrong check digit
    [InlineData("12345")]
    [InlineData("")]
    [InlineData(null)]
    public void InvalidIsbns_AreRejected(string? isbn) => Assert.False(IsbnValidator.IsValid(isbn));
}
