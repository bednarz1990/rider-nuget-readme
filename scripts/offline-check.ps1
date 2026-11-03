param([Parameter(Mandatory=$true)][string]$RiderHome)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sdkRoot = (Resolve-Path -LiteralPath $RiderHome).Path
$javac = Join-Path $sdkRoot 'jbr\bin\javac.exe'
$java = Join-Path $sdkRoot 'jbr\bin\java.exe'
$output = Join-Path $projectRoot ('build\offline-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $output 'classes'
$testClasses = Join-Path $output 'test-classes'
New-Item -ItemType Directory -Force $classes,$testClasses | Out-Null
$classpath = Join-Path $sdkRoot 'lib\*'
$sources = Get-ChildItem (Join-Path $projectRoot 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName
& $javac -encoding UTF-8 --release 17 -cp $classpath -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Production compilation failed.' }
$testSource = Join-Path $projectRoot 'src\test\java\io\github\bednarz1990\nuget\CoreChecks.java'
& $javac -encoding UTF-8 --release 17 -cp "$classes;$classpath" -d $testClasses $testSource
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& $java '-Djava.awt.headless=true' -cp "$testClasses;$classes;$classpath" io.github.bednarz1990.nuget.CoreChecks 2>&1 | Tee-Object -FilePath (Join-Path $output 'tests.txt')
if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
Copy-Item -Path (Join-Path $projectRoot 'src\main\resources\*') -Destination $classes -Recurse
Add-Type -AssemblyName System.IO.Compression.FileSystem
$plugin = Join-Path $output 'package\rider-nuget-readme\lib'
New-Item -ItemType Directory -Force $plugin | Out-Null
[IO.Compression.ZipFile]::CreateFromDirectory($classes, (Join-Path $plugin 'rider-nuget-readme-0.1.0.jar'))
$zip = Join-Path $output 'rider-nuget-readme-0.1.0-offline-candidate.zip'
[IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $output 'package'), $zip)
Write-Output "Offline candidate only; target Rider compatibility NOT verified: $zip"
