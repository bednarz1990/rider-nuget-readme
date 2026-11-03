# NuGet README Demo

Przykladowy pakiet .NET z dokumentacja umieszczona wewnatrz pliku `.nupkg`.

## Uzycie

```csharp
using NuGetReadmeDemo;

Console.WriteLine(Greeter.Hello("Lukasz"));
```

Wynik: `Czesc, Lukasz! README z pakietu NuGet dziala.`

## Funkcje

- Prosta metoda powitalna.
- Walidacja pustego imienia.
- README zawierajacy naglowki, kod, liste, tabele i link.

| Metoda | Wynik |
| --- | --- |
| `Greeter.Hello("Lukasz")` | Powitanie |
| `Greeter.Hello("")` | ArgumentException |

## Wymagania

.NET 10.

[Kod wtyczki NuGet README Preview](https://github.com/bednarz1990/rider-nuget-readme)
