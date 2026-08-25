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
            'pack.mcmeta',
            'LICENSE.md'
        )) {
            if ($required -notin $entries) {
                throw "Production JAR is missing $required"
            }
        }

        $packEntry = $archive.GetEntry('pack.mcmeta')
        $packReader = [System.IO.StreamReader]::new($packEntry.Open())
        try { $packText = $packReader.ReadToEnd() } finally { $packReader.Dispose() }
        try { $packMetadata = $packText | ConvertFrom-Json -ErrorAction Stop } catch {
            throw "Production JAR contains an invalid pack.mcmeta: $($_.Exception.Message)"
        }
        if ($packMetadata.pack.pack_format -ne 15 -or [string]::IsNullOrWhiteSpace([string]$packMetadata.pack.description)) {
            throw 'Production JAR pack.mcmeta must declare pack format 15 and a non-empty description'
        }

        $moduleRefmaps = @(
            @{
                Jar = 'META-INF/jarjar/dev.simulated_team.simulated.simulated-forge-1.20.1-1.3.1-port.1.jar'
                Refmap = 'simulated.refmap.json'
                Configs = @('simulated.mixins.json', 'simulated-forge.mixins.json')
                MappingChecks = @(
                    @{
                        Mixin = 'dev/simulated_team/simulated/mixin/world_presets/PrimaryLevelDataMixin'
                        Member = 'parse'
                        Target = 'Lnet/minecraft/world/level/storage/PrimaryLevelData;m_78530_('
                    },
                    @{
                        Mixin = 'dev/simulated_team/simulated/mixin/world_presets/PrimaryLevelDataMixin'
                        Member = 'setTagData'
                        Target = 'Lnet/minecraft/world/level/storage/PrimaryLevelData;m_78545_('
                    }
                )
            },
            @{
                Jar = 'META-INF/jarjar/dev.eriksonn.aeronautics.aeronautics-forge-1.20.1-1.3.1-port.1.jar'
                Refmap = 'aeronautics.refmap.json'
                Configs = @('aeronautics.mixins.json', 'aeronautics-forge.mixins.json')
                MappingChecks = @()
            },
            @{
                Jar = 'META-INF/jarjar/dev.ryanhcode.offroad.offroad-forge-1.20.1-1.3.1-port.1.jar'
                Refmap = 'offroad.refmap.json'
                Configs = @('offroad.mixins.json', 'offroad-forge.mixins.json')
                MappingChecks = @()
            }
        )

        foreach ($module in $moduleRefmaps) {
            $moduleEntry = $archive.GetEntry($module.Jar)
            $memory = [System.IO.MemoryStream]::new()
            $moduleStream = $moduleEntry.Open()
            try { $moduleStream.CopyTo($memory) } finally { $moduleStream.Dispose() }
            $memory.Position = 0
            $nested = [System.IO.Compression.ZipArchive]::new(
                $memory,
                [System.IO.Compression.ZipArchiveMode]::Read,
                $false)
            try {
                $nestedPackEntry = $nested.GetEntry('pack.mcmeta')
                if ($null -eq $nestedPackEntry) {
                    throw "$($module.Jar) is missing pack.mcmeta"
                }
                $nestedPackReader = [System.IO.StreamReader]::new($nestedPackEntry.Open())
                try { $nestedPackText = $nestedPackReader.ReadToEnd() } finally { $nestedPackReader.Dispose() }
                try { $nestedPackMetadata = $nestedPackText | ConvertFrom-Json -ErrorAction Stop } catch {
                    throw "$($module.Jar) contains an invalid pack.mcmeta: $($_.Exception.Message)"
                }
                if ($nestedPackMetadata.pack.pack_format -ne 15 -or [string]::IsNullOrWhiteSpace([string]$nestedPackMetadata.pack.description)) {
                    throw "$($module.Jar) pack.mcmeta must declare pack format 15 and a non-empty description"
                }

                $refmapEntry = $nested.GetEntry($module.Refmap)
                if ($null -eq $refmapEntry -or $refmapEntry.Length -lt 32) {
                    throw "$($module.Jar) is missing a non-empty $($module.Refmap)"
                }

                $refmapReader = [System.IO.StreamReader]::new($refmapEntry.Open())
                try { $refmapText = $refmapReader.ReadToEnd() } finally { $refmapReader.Dispose() }
                try { $refmap = $refmapText | ConvertFrom-Json -ErrorAction Stop } catch {
                    throw "$($module.Jar) contains an invalid $($module.Refmap): $($_.Exception.Message)"
                }
                if ($null -eq $refmap.mappings -or @($refmap.mappings.PSObject.Properties).Count -eq 0) {
                    throw "$($module.Jar) contains an empty $($module.Refmap) mapping set"
                }

                foreach ($check in $module.MappingChecks) {
                    $mixinMappings = $refmap.data.searge.PSObject.Properties[$check.Mixin].Value
                    $mappedTarget = if ($null -eq $mixinMappings) {
                        $null
                    } else {
                        $mixinMappings.PSObject.Properties[$check.Member].Value
                    }
                    if ([string]::IsNullOrWhiteSpace([string]$mappedTarget) -or -not $mappedTarget.Contains($check.Target)) {
                        throw "$($module.Jar) does not map $($check.Mixin)::$($check.Member) to $($check.Target)"
                    }
                }

                foreach ($configName in $module.Configs) {
                    $configEntry = $nested.GetEntry($configName)
                    if ($null -eq $configEntry) {
                        throw "$($module.Jar) is missing $configName"
                    }
                    $configReader = [System.IO.StreamReader]::new($configEntry.Open())
                    try { $configText = $configReader.ReadToEnd() } finally { $configReader.Dispose() }
                    try { $config = $configText | ConvertFrom-Json -ErrorAction Stop } catch {
                        throw "$($module.Jar) contains an invalid ${configName}: $($_.Exception.Message)"
                    }
                    if ($config.refmap -ne $module.Refmap) {
                        throw "$($module.Jar) config $configName does not reference $($module.Refmap)"
                    }
                }
            }
            finally {
                $nested.Dispose()
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
$arguments = @('--no-daemon', '--stacktrace', '-p', $repositoryPath, "-Psable_jar=$SableJar", 'clean', ':aeronautics-bundled:build')
if ($Offline) { $arguments += '--offline' }
if ($Mode -eq 'quick') { $arguments += @('-x', 'test') }

& $gradle @arguments
if ($LASTEXITCODE -ne 0) { throw "Simulated build failed with exit code $LASTEXITCODE" }

if ($Mode -eq 'test') {
    $testArguments = @('--no-daemon', '--stacktrace', '-p', $repositoryPath, "-Psable_jar=$SableJar", ':all-forge:runGameTest')
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
