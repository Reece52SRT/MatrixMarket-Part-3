namespace MatrixMarket.Api.DTOs;

// Consistent error/info body: { "message": "..." } - the app shows this text to the user.
public record ApiMessage(string Message);
