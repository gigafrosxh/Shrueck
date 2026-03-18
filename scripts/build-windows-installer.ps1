param(
    [string]$PlatformTag = "windows-x64",
    [string]$PackageType = "exe",
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $projectRoot ".tools"
$mavenVersion = "3.9.10"
$mavenHome = Join-Path $toolsDir ("apache-maven-" + $mavenVersion)
$localMaven = Join-Path $mavenHome "bin\mvn.cmd"
$inputDir = Join-Path $projectRoot "target\jpackage\input"
$distDir = Join-Path $projectRoot ("dist\" + $PlatformTag)
$wixDir = Join-Path $toolsDir "wix314"
$wixZip = Join-Path $toolsDir "wix314-binaries.zip"
$wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"

function Resolve-MavenCommand {
    $globalMaven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -ne $globalMaven) {
        return $globalMaven.Source
    }

    if (Test-Path $localMaven) {
        return $localMaven
    }

    throw "Kein Maven gefunden. Bitte zuerst run-game.ps1 oder einen Maven-Bootstrap ausfuehren."
}

function Get-MainJar {
    $jar = Get-ChildItem -Path (Join-Path $projectRoot "target") -Filter "*.jar" |
        Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Kein Haupt-JAR in target gefunden."
    }

    return $jar
}

function Resolve-WixToolset {
    $existingCandle = Get-Command candle.exe -ErrorAction SilentlyContinue
    if ($null -ne $existingCandle) {
        return Split-Path -Parent $existingCandle.Source
    }

    if (-not (Test-Path $wixDir)) {
        New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
        Invoke-WebRequest -Uri $wixUrl -OutFile $wixZip
        Expand-Archive -Path $wixZip -DestinationPath $wixDir -Force
        Remove-Item $wixZip -Force
    }

    $candle = Get-ChildItem -Path $wixDir -Recurse -Filter candle.exe | Select-Object -First 1
    if ($null -eq $candle) {
        throw "WiX wurde nicht gefunden."
    }

    $wixBin = Split-Path -Parent $candle.FullName
    $env:PATH = $wixBin + [System.IO.Path]::PathSeparator + $env:PATH
    return $wixBin
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage ist nicht verfuegbar. Bitte ein JDK 21+ mit jpackage verwenden."
}

$mavenCommand = Resolve-MavenCommand
$null = Resolve-WixToolset

Push-Location $projectRoot
try {
    Remove-Item $inputDir -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $distDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null

    & $mavenCommand -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime ("-DoutputDirectory=" + $inputDir)

    $mainJar = Get-MainJar
    Copy-Item $mainJar.FullName -Destination $inputDir -Force

    $jpackageArgs = @(
        '--type', $PackageType,
        '--dest', $distDir,
        '--input', $inputDir,
        '--name', 'ShrueckLAN',
        '--app-version', $AppVersion,
        '--vendor', 'Shrueck',
        '--description', 'Shrueck LAN Multiplayer',
        '--main-jar', $mainJar.Name,
        '--main-class', 'at.shrueck.net.game.ShrueckGameLauncher',
        '--java-options', '-Dfile.encoding=UTF-8',
        '--win-dir-chooser',
        '--win-menu',
        '--win-shortcut',
        '--win-menu-group', 'Shrueck'
    )

    & jpackage @jpackageArgs

    $artifact = Get-ChildItem -Path $distDir -Filter ("*." + $PackageType) | Select-Object -First 1
    if ($null -eq $artifact) {
        throw "Kein $PackageType-Installer wurde erzeugt."
    }

    $targetName = "ShrueckLAN-" + $AppVersion + "-" + $PlatformTag + "." + $PackageType
    $targetPath = Join-Path $distDir $targetName
    if ($artifact.FullName -ne $targetPath) {
        Move-Item -Path $artifact.FullName -Destination $targetPath -Force
    }

    Write-Host "Installer erzeugt: $targetPath"
}
finally {
    Pop-Location
}