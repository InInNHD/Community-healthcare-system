
[CmdletBinding()]
param(
    [string]$JavaHome,
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'

function Get-JavaMajorVersion([string]$Candidate) {
    if ([string]::IsNullOrWhiteSpace($Candidate)) {
        return $null
    }

    $javac = Join-Path $Candidate 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javac)) {
        return $null
    }

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'SilentlyContinue'
        $versionText = (& $javac -version 2>&1 | Out-String).Trim()
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($versionText -match 'javac\s+(\d+)') {
        return [int]$Matches[1]
    }
    return $null
}

$candidates = [System.Collections.Generic.List[string]]::new()
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $candidates.Add($JavaHome)
}
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $candidates.Add($env:JAVA_HOME)
}

$javaRoot = 'C:\Program Files\Java'
if (Test-Path -LiteralPath $javaRoot) {
    Get-ChildItem -LiteralPath $javaRoot -Directory -Filter 'jdk*' |
        Sort-Object Name -Descending |
        ForEach-Object { $candidates.Add($_.FullName) }
}

$selectedJavaHome = $null
foreach ($candidate in $candidates | Select-Object -Unique) {
    $major = Get-JavaMajorVersion $candidate
    if ($null -ne $major -and $major -ge 17) {
        $selectedJavaHome = (Resolve-Path -LiteralPath $candidate).Path
        break
    }
}

if ($null -eq $selectedJavaHome) {
    throw 'JDK 17 or newer was not found. Install JDK 17 or pass its directory with -JavaHome.'
}

$env:JAVA_HOME = $selectedJavaHome
$env:Path = "$(Join-Path $selectedJavaHome 'bin');$env:Path"

Write-Host "Using JAVA_HOME=$selectedJavaHome"
$selectedMajor = Get-JavaMajorVersion $selectedJavaHome
Write-Host "Using JDK major version $selectedMajor"

if ($CheckOnly) {
    Write-Host 'JDK version check passed.'
    return
}

Push-Location $PSScriptRoot
try {
    & mvn.cmd -pl backend spring-boot:run '-Dspring-boot.run.profiles=demo'
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
