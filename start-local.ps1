[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$RefreshDependencies
)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$backendDirectory = Join-Path $projectRoot 'backend'
$frontendDirectory = Join-Path $projectRoot 'frontend'
$composeFile = Join-Path $projectRoot 'compose.local.yml'
$envFile = Join-Path $projectRoot '.env'
$envExampleFile = Join-Path $projectRoot '.env.example'

function Assert-Command {
    param([Parameter(Mandatory = $true)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found. Install it and add it to PATH first."
    }
}

function Import-DotEnv {
    param([Parameter(Mandatory = $true)][string]$Path)

    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmedLine = $line.Trim()
        if (-not $trimmedLine -or $trimmedLine.StartsWith('#')) {
            continue
        }

        $parts = $trimmedLine -split '=', 2
        if ($parts.Count -eq 2 -and $parts[0].Trim()) {
            Set-Item -Path ("Env:" + $parts[0].Trim()) -Value $parts[1].Trim()
        }
    }
}

function Get-JavaMajorVersion {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    $javaExecutable = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExecutable)) {
        return $null
    }

    # java -version writes normal version information to stderr. Windows
    # PowerShell can turn that output into a terminating NativeCommandError
    # when the script-wide preference is Stop, so relax it only for this call.
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $versionOutput = (& $javaExecutable -version 2>&1 | Out-String)
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($versionOutput -match 'version "(?:1\.)?(\d+)') {
        return [int]$Matches[1]
    }

    return $null
}

function Find-CompatibleJdk {
    $candidatePaths = @()

    if ($env:JAVA_HOME) {
        $candidatePaths += $env:JAVA_HOME
    }

    $searchRoots = @(
        (Join-Path $env:USERPROFILE '.jdks'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
    )

    foreach ($searchRoot in $searchRoots) {
        if (Test-Path -LiteralPath $searchRoot) {
            $candidatePaths += Get-ChildItem -LiteralPath $searchRoot -Directory -Filter 'jdk*' |
                Select-Object -ExpandProperty FullName
        }
    }

    $availableJdks = @()
    foreach ($candidatePath in ($candidatePaths | Select-Object -Unique)) {
        $majorVersion = Get-JavaMajorVersion -JavaHome $candidatePath
        if ($majorVersion -ge 21) {
            $availableJdks += [PSCustomObject]@{
                Home = $candidatePath
                MajorVersion = $majorVersion
            }
        }
    }

    $jdk25OrNewer = $availableJdks |
        Where-Object { $_.MajorVersion -ge 25 } |
        Sort-Object MajorVersion |
        Select-Object -First 1

    if ($jdk25OrNewer) {
        return $jdk25OrNewer
    }

    # Without JDK 25, prefer JDK 21 so the runtime matches the temporary
    # Java 21 compilation target instead of selecting an unrelated newer JDK.
    return $availableJdks |
        Sort-Object MajorVersion |
        Select-Object -First 1
}

try {
    Write-Host 'Checking the local development environment...' -ForegroundColor Cyan

    Assert-Command -Name 'docker'
    Assert-Command -Name 'mvn'
    Assert-Command -Name 'npm'

    if (-not (Test-Path -LiteralPath $envFile)) {
        if (-not (Test-Path -LiteralPath $envExampleFile)) {
            throw '.env and .env.example were not found.'
        }

        Copy-Item -LiteralPath $envExampleFile -Destination $envFile
        Write-Host 'Created local .env from .env.example.' -ForegroundColor Green
    }

    Import-DotEnv -Path $envFile

    $selectedJdk = Find-CompatibleJdk
    if (-not $selectedJdk) {
        throw 'JDK 21 or newer was not found. Install JDK 25 (recommended) or JDK 21 first.'
    }

    $env:JAVA_HOME = $selectedJdk.Home
    $env:Path = "$($selectedJdk.Home)\bin;$env:Path"

    Write-Host ("Using JDK {0}: {1}" -f $selectedJdk.MajorVersion, $selectedJdk.Home) -ForegroundColor Green

    # Docker Desktop may print harmless WSL capability warnings to stderr
    # (for example, missing blkio throttling support). Check only its exit code.
    & $env:ComSpec /d /c 'docker info >nul 2>&1'
    $dockerInfoExitCode = $LASTEXITCODE
    if ($dockerInfoExitCode -ne 0) {
        throw 'Docker is unavailable. Start Docker Desktop first.'
    }

    Write-Host 'Starting MySQL...' -ForegroundColor Cyan
    & docker compose -f $composeFile up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw 'The MySQL container failed to start.'
    }

    $mysqlReady = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        $healthStatus = (& docker inspect --format '{{.State.Health.Status}}' xmreader-mysql 2>$null | Out-String).Trim()
        if ($healthStatus -eq 'healthy') {
            $mysqlReady = $true
            break
        }

        Write-Progress -Activity 'Waiting for MySQL' -Status "Current status: $healthStatus" -PercentComplete (($attempt / 60) * 100)
        Start-Sleep -Seconds 2
    }
    Write-Progress -Activity 'Waiting for MySQL' -Completed

    if (-not $mysqlReady) {
        throw 'Timed out waiting for MySQL. Run docker compose -f compose.local.yml ps to inspect it.'
    }

    $crawlerReady = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        $healthStatus = (& docker inspect --format '{{.State.Health.Status}}' xmreader-sonovel-adapter 2>$null | Out-String).Trim()
        if ($healthStatus -eq 'healthy') {
            $crawlerReady = $true
            break
        }

        Write-Progress -Activity 'Waiting for the crawler adapter' -Status "Current status: $healthStatus" -PercentComplete (($attempt / 60) * 100)
        Start-Sleep -Seconds 2
    }
    Write-Progress -Activity 'Waiting for the crawler adapter' -Completed

    if (-not $crawlerReady) {
        throw 'Timed out waiting for the crawler adapter. Run docker compose -f compose.local.yml logs sonovel-adapter to inspect it.'
    }

    if ($selectedJdk.MajorVersion -ge 25) {
        $backendCommand = 'title xmreader backend && mvn clean spring-boot:run'
    } else {
        $backendCommand = 'title xmreader backend && mvn "-Djava.version=21" clean spring-boot:run'
        Write-Host 'JDK 25 was not found. The backend will temporarily compile for Java 21.' -ForegroundColor Yellow
    }

    if ($RefreshDependencies) {
        $frontendCommand = 'title xmreader frontend && npm ci && npm run dev'
    } else {
        $frontendCommand = 'title xmreader frontend && if exist node_modules (npm run dev) else (npm ci && npm run dev)'
    }

    Write-Host 'Opening backend and frontend terminals...' -ForegroundColor Cyan
    Start-Process -FilePath $env:ComSpec -ArgumentList '/k', $backendCommand -WorkingDirectory $backendDirectory
    Start-Process -FilePath $env:ComSpec -ArgumentList '/k', $frontendCommand -WorkingDirectory $frontendDirectory

    Write-Host ''
    Write-Host 'xmreader local services have been launched:' -ForegroundColor Green
    Write-Host '  Frontend: http://localhost:5173'
    Write-Host '  Backend: http://localhost:8080'
    Write-Host '  MySQL: localhost:3306'
    Write-Host '  Crawler adapter: http://localhost:7765'
    Write-Host ''
    Write-Host 'Stop frontend/backend: press Ctrl+C in their terminal windows.'
    Write-Host 'Stop MySQL: docker compose -f compose.local.yml stop'

    if (-not $NoBrowser) {
        $frontendReady = $false
        for ($attempt = 1; $attempt -le 30; $attempt++) {
            try {
                Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2 | Out-Null
                $frontendReady = $true
                break
            } catch {
                Start-Sleep -Seconds 1
            }
        }

        if ($frontendReady) {
            Start-Process 'http://localhost:5173'
        } else {
            Write-Host 'The frontend is still starting. Open http://localhost:5173 in a moment.' -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host ''
    Write-Host ("Startup failed: {0}" -f $_.Exception.Message) -ForegroundColor Red
    exit 1
}
