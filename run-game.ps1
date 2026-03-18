$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsDir = Join-Path $projectRoot ".tools"
$mavenVersion = "3.9.10"
$mavenHome = Join-Path $toolsDir ("apache-maven-" + $mavenVersion)
$localMaven = Join-Path $mavenHome "bin\mvn.cmd"

function Resolve-MavenCommand {
    $globalMaven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -ne $globalMaven) {
        return $globalMaven.Source
    }

    if (Test-Path $localMaven) {
        return $localMaven
    }

    New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
    $archivePath = Join-Path $toolsDir ("apache-maven-" + $mavenVersion + "-bin.zip")
    $downloadUrl = "https://archive.apache.org/dist/maven/maven-3/" + $mavenVersion + "/binaries/apache-maven-" + $mavenVersion + "-bin.zip"

    Write-Host "Lade Maven $mavenVersion herunter ..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath
    Expand-Archive -Path $archivePath -DestinationPath $toolsDir -Force
    Remove-Item $archivePath -Force

    return $localMaven
}

$mavenCommand = Resolve-MavenCommand
$execArgs = if ($args.Count -gt 0) { $args -join ' ' } else { "" }

Push-Location $projectRoot
try {
    if ([string]::IsNullOrWhiteSpace($execArgs)) {
        & $mavenCommand -q exec:java
    }
    else {
        & $mavenCommand -q exec:java ("-Dexec.args=" + $execArgs)
    }
}
finally {
    Pop-Location
}