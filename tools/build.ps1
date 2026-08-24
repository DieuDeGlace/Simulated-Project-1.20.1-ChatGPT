[CmdletBinding()]
param(
    [ValidateSet('release', 'test', 'quick', 'audit')]
    [string]$Mode = 'release',
    [string]$SableJar,
    [string]$SableSha256,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$repositoryPath = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$toolCache = Join-Path $repositoryPath '.build-tools'
$distributionDirectory = Join-Path $repositoryPath 'dist'
$jdkVersion = '17.0.20.1'
$jdkSha256 = '3D9006956FC8AF5601CD24FFC4F468BEF48279C7EBD8171B9BDF90D0AABFBF1F'

function Get-Sha256 {
    param([Parameter(Mandatory)][string]$Path)
    $stream = [System.IO.File]::OpenRead($Path)
    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    try {
        return (($algorithm.ComputeHash($stream) | ForEach-Object { $_.ToString('x2') }) -join '').ToUpperInvariant()
    }
    finally {
        $algorithm.Dispose()
        $stream.Dispose()
    }
}

function Get-JavaMajorVersion {
    param([Parameter(Mandatory)][string]$JavaExecutable)
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $text = (& $JavaExecutable -version 2>&1 | Out-String)
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($LASTEXITCODE -ne 0 -or $text -notmatch 'version\s+"(?<major>\d+)') {
        return $null
    }
    return [int]$Matches.major
}

function Find-Java17 {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe'))
    }
    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($pathJava) {
        $candidates.Add($pathJava.Source)
    }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ((Test-Path -LiteralPath $candidate -PathType Leaf) -and (Get-JavaMajorVersion $candidate) -eq 17) {
            return Split-Path -Parent (Split-Path -Parent $candidate)
        }
    }
    return $null
}

function Install-Java17 {
    if (-not [Environment]::Is64BitOperatingSystem -or $env:OS -ne 'Windows_NT') {
        throw 'Automatic JDK installation supports 64-bit Windows. Install Java 17 and run again.'
    }
    New-Item -ItemType Directory -Path $toolCache -Force | Out-Null
    $archive = Join-Path $toolCache "microsoft-jdk-$jdkVersion-windows-x64.zip"
    $extractRoot = Join-Path $toolCache "microsoft-jdk-$jdkVersion-windows-x64"
    $java = Get-ChildItem -LiteralPath $extractRoot -Filter java.exe -File -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    if ($java -and (Get-JavaMajorVersion $java.FullName) -eq 17) {
        return Split-Path -Parent (Split-Path -Parent $java.FullName)
    }
    if ($Offline) {
        throw 'Java 17 was not found and offline mode cannot download it.'
    }
    Invoke-WebRequest -UseBasicParsing -Uri "https://aka.ms/download-jdk/microsoft-jdk-$jdkVersion-windows-x64.zip" -OutFile $archive
    $actualHash = Get-Sha256 $archive
    if ($actualHash -ne $jdkSha256) {
        [System.IO.File]::Delete($archive)
        throw "JDK checksum verification failed. Expected $jdkSha256, received $actualHash."
    }
    if (Test-Path -LiteralPath $extractRoot) {
        Remove-Item -LiteralPath $extractRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $extractRoot -Force | Out-Null
    Expand-Archive -LiteralPath $archive -DestinationPath $extractRoot
    $java = Get-ChildItem -LiteralPath $extractRoot -Filter java.exe -File -Recurse |
        Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    if (-not $java -or (Get-JavaMajorVersion $java.FullName) -ne 17) {
        throw 'The verified JDK archive did not contain Java 17.'
    }
    return Split-Path -Parent (Split-Path -Parent $java.FullName)
}

function Assert-SableJar {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Sable JAR does not exist: $Path"
    }
    if ($SableSha256) {
        $actualHash = Get-Sha256 $Path
        if ($actualHash -ne $SableSha256.ToUpperInvariant()) {
            throw "Sable checksum mismatch. Expected $SableSha256, received $actualHash."
        }
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Path).Path)
    try {
        foreach ($required in @(
            'META-INF/mods.toml',
            'dev/ryanhcode/sable/Sable.class',
            'dev/ryanhcode/sable/physics/ReactionWheelManager.class'
        )) {
            if ($null -eq $archive.GetEntry($required)) {
                throw "Sable dependency is missing $required"
            }
        }
        $entry = $archive.GetEntry('META-INF/mods.toml')
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try { $modsToml = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($modsToml -notmatch '(?m)^version\s*=\s*"2\.0\.5-port\.1"\s*$') {
            throw 'Sable dependency is not version 2.0.5-port.1.'
        }
    }
    finally {
        $archive.Dispose()
    }
}

function Assert-ProductionJar {
    param([Parameter(Mandatory)][string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entries = @($archive.Entries | ForEach-Object { $_.FullName.Replace('\', '/') })
        foreach ($required in @(
            'META-INF/mods.toml',
            'META-INF/jarjar/metadata.json',
            'META-INF/jarjar/dev.simulated_team.simulated.simulated-forge-1.20.1-1.3.1-port.1.jar',
            'META-INF/jarjar/dev.eriksonn.aeronautics.aeronautics-forge-1.20.1-1.3.1-port.1.jar',
            'META-INF/jarjar/dev.ryanhcode.offroad.offroad-forge-1.20.1-1.3.1-port.1.jar',
            'LICENSE.md'
        )) {
            if ($required -notin $entries) {
                throw "Production JAR is missing $required"
            }
        }
        $forbidden = @($entries | Where-Object {
            $_ -match '(^|/)(validation|gametest|evidence|construction|logs?)(/|$)' -or
            $_ -match '(^|/)DedicatedServerLifecycleValidation\.class$'
        })
        if ($forbidden.Count -gt 0) {
            throw "Development-only content entered the production JAR: $($forbidden -join ', ')"
        }
    }
    finally {
        $archive.Dispose()
    }
}

& (Join-Path $PSScriptRoot 'audit-public-tree.ps1') -Repository $repositoryPath
if ($Mode -eq 'audit') {
    return
}

if (-not $SableJar) { $SableJar = $env:SIMULATED_SABLE_JAR }
if (-not $SableJar) {
    throw 'Provide the exact Sable 2.0.5-port.1 JAR with -SableJar or SIMULATED_SABLE_JAR.'
}
$SableJar = (Resolve-Path -LiteralPath $SableJar).Path
Assert-SableJar $SableJar

$javaHome = Find-Java17
if (-not $javaHome) { $javaHome = Install-Java17 }
$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome 'bin') + [IO.Path]::PathSeparator + $env:Path

$gradle = Join-Path $repositoryPath 'gradlew.bat'
$arguments = @('--no-daemon', '--stacktrace', "-Psable_jar=$SableJar", 'clean', ':aeronautics-bundled:build')
if ($Offline) { $arguments += '--offline' }
if ($Mode -eq 'quick') { $arguments += @('-x', 'test') }

& $gradle @arguments
if ($LASTEXITCODE -ne 0) { throw "Simulated build failed with exit code $LASTEXITCODE" }

if ($Mode -eq 'test') {
    $testArguments = @('--no-daemon', '--stacktrace', "-Psable_jar=$SableJar", ':all-forge:runGameTest')
    if ($Offline) { $testArguments += '--offline' }
    & $gradle @testArguments
    if ($LASTEXITCODE -ne 0) { throw "Simulated GameTest failed with exit code $LASTEXITCODE" }
}

$artifacts = @(Get-ChildItem -LiteralPath (Join-Path $repositoryPath 'aeronautics-bundled\build\libs') -Filter 'create-aeronautics-bundled-1.20.1-*.jar' -File |
    Where-Object { $_.Name -notmatch '-(sources|dev|slim)\.jar$' })
if ($artifacts.Count -ne 1) {
    throw "Expected one production bundle, found $($artifacts.Count)."
}
$artifact = $artifacts[0]
Assert-ProductionJar $artifact.FullName

New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null
Get-ChildItem -LiteralPath $distributionDirectory -File -ErrorAction SilentlyContinue | Remove-Item -Force
$destination = Join-Path $distributionDirectory $artifact.Name
Copy-Item -LiteralPath $artifact.FullName -Destination $destination
$hash = Get-Sha256 $destination
[System.IO.File]::WriteAllText(
    (Join-Path $distributionDirectory 'SHA256SUMS.txt'),
    "$hash  $($artifact.Name)`n",
    [System.Text.UTF8Encoding]::new($false))

Write-Host "Build complete: $destination"
Write-Host "SHA-256: $hash"
