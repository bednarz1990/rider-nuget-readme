# Przyklad pakietu NuGet z README

Wymagany SDK .NET 10. Z katalogu glownego repozytorium:

```powershell
dotnet pack examples/ReadmeDemo/ReadmeDemo.csproj -c Release -o artifacts/nuget
```

Wynik: `artifacts/nuget/Bednarz1990.NuGetReadmeDemo.0.1.0.nupkg`.
Archiwum zawiera biblioteke, README.md i deklaracje `<readme>README.md</readme>`
w pliku .nuspec. Mozesz otworzyc je jak ZIP, aby sprawdzic zawartosc.

W projekcie testowym mozna zainstalowac lokalny pakiet:

```powershell
dotnet add package Bednarz1990.NuGetReadmeDemo --version 0.1.0 --source C:\sciezka\do\repo\artifacts\nuget
```

## Podglad we wtyczce

Wtyczka 0.1.0 obsluguje tylko publiczny NuGet.org, nie lokalne pliki .nupkg
ani lokalne zrodla pakietow. Samo zainstalowanie lokalnego pakietu nie sprawi,
ze bedzie go mozna wyszukac w jej oknie.

Aby od razu przetestowac podglad, otworz projekt w Riderze i wybierz
View > Tool Windows > NuGet README. Wyszukaj Serilog, wybierz pakiet i wersje.
Sprawdz zakladki Preview i Markdown. Mozesz tez uzyc Newtonsoft.Json lub Dapper.

Wlasny przyklad pojawi sie w wyszukiwaniu dopiero po publikacji i indeksacji
na NuGet.org. Ten przyklad nie jest automatycznie publikowany.
