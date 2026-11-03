namespace NuGetReadmeDemo;

public static class Greeter
{
    public static string Hello(string name)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(name);
        return $"Czesc, {name}! README z pakietu NuGet dziala.";
    }
}
