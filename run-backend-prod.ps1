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

function Require-EnvironmentVariable([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable $Name is missing in this PowerShell process."
    }
    return $value
}

$dbUrl = Require-EnvironmentVariable 'DB_URL'
$null = Require-EnvironmentVariable 'DB_USERNAME'
$null = Require-EnvironmentVariable 'DB_PASSWORD'
$null = Require-EnvironmentVariable 'JWT_ACTIVE_KID'
$jwtSecret = Require-EnvironmentVariable 'JWT_ACTIVE_SECRET'
$null = Require-EnvironmentVariable 'PORTAL_ORGANIZATION_NAME'
$null = Require-EnvironmentVariable 'PORTAL_SERVICE_PHONE'
$null = Require-EnvironmentVariable 'PORTAL_SERVICE_HOURS'
$null = Require-EnvironmentVariable 'PORTAL_EMERGENCY_PHONE'

if (-not $dbUrl.StartsWith('jdbc:mysql://', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'DB_URL must start with jdbc:mysql://'
}
if ([Text.Encoding]::UTF8.GetByteCount($jwtSecret) -lt 32) {
    throw 'JWT_ACTIVE_SECRET must contain at least 32 UTF-8 bytes.'
}
if ($jwtSecret -eq 'demo-only-active-jwt-key-2026-change-before-production') {
    throw 'The demo JWT secret is forbidden in prod mode.'
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

$jarPath = Join-Path $PSScriptRoot 'backend\target\healthcare-backend-2.0.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw 'Backend JAR was not found. Run: mvn.cmd -pl backend clean package'
}

$env:JAVA_HOME = $selectedJavaHome
$env:Path = "$(Join-Path $selectedJavaHome 'bin');$env:Path"
$env:SPRING_PROFILES_ACTIVE = 'prod'

Write-Host "Using JAVA_HOME=$selectedJavaHome"
Write-Host "Using DB_URL=$dbUrl"
Write-Host 'Prod configuration check passed.'

if ($CheckOnly) {
    return
}

& (Join-Path $selectedJavaHome 'bin\java.exe') -jar $jarPath '--spring.profiles.active=prod'
if ($LASTEXITCODE -ne 0) {
    throw "Backend exited with code $LASTEXITCODE"
}
