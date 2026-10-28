namespace MatrixMarket.Api.Services;

// Decides where the SQLite + LiteDB files and locally stored images live.
public static class StoragePaths
{
    public static string ResolveDataDirectory(IConfiguration config, string contentRoot)
    {
        var configured = config["Storage:DataDirectory"];
        if (!string.IsNullOrWhiteSpace(configured)) return configured;

        // Azure App Service (Linux and Windows) exposes a persistent /home share
        // and always sets WEBSITE_SITE_NAME.
        var home = Environment.GetEnvironmentVariable("HOME");
        if (!string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("WEBSITE_SITE_NAME"))
            && !string.IsNullOrWhiteSpace(home))
        {
            return Path.Combine(home, "data", "matrixmarket");
        }

        return Path.Combine(contentRoot, "App_Data");
    }
}
