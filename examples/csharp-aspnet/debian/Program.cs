using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddSingleton<PasteStore>();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

// Literal routes are registered before the catch-all "/{code}" route.
app.MapGet("/health", () => Results.Json(new { status = "ok" }));

app.MapGet("/", () => Results.Json(new { service = "pastebin-csharp", docs = "/swagger" }));

app.MapPost("/paste", (PasteRequest request, PasteStore store) =>
{
    var url = request.Url;

    if (string.IsNullOrWhiteSpace(url))
    {
        return Results.Json(new { error = "url is required" }, statusCode: 400);
    }

    if (!url.StartsWith("http://", StringComparison.Ordinal) &&
        !url.StartsWith("https://", StringComparison.Ordinal))
    {
        return Results.Json(new { error = "url must be a valid http(s) URL" }, statusCode: 400);
    }

    var code = store.Create(url);
    return Results.Json(new
    {
        code,
        short_url = "/" + code,
        long_url = url
    }, statusCode: 201);
});

app.MapGet("/{code}", (string code, PasteStore store) =>
{
    // Validate the 8-char [A-Za-z0-9] shape inside the handler.
    if (!Regex.IsMatch(code, "^[A-Za-z0-9]{8}$"))
    {
        return Results.Json(new { error = "not found" }, statusCode: 404);
    }

    if (store.TryGet(code, out var longUrl))
    {
        return Results.Redirect(longUrl!);
    }

    return Results.Json(new { error = "not found" }, statusCode: 404);
});

app.Run();

record PasteRequest(string? Url);

class PasteStore
{
    private const string Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private readonly ConcurrentDictionary<string, string> _map = new(StringComparer.Ordinal);

    public string Create(string url)
    {
        while (true)
        {
            var code = GenerateCode();
            if (_map.TryAdd(code, url))
            {
                return code;
            }
        }
    }

    public bool TryGet(string code, out string? url) => _map.TryGetValue(code, out url);

    private static string GenerateCode()
    {
        Span<byte> bytes = stackalloc byte[8];
        RandomNumberGenerator.Fill(bytes);

        var sb = new StringBuilder(8);
        foreach (var b in bytes)
        {
            sb.Append(Alphabet[b % Alphabet.Length]);
        }

        return sb.ToString();
    }
}
