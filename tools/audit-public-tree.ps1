[CmdletBinding()]
param(
    [string]$Repository,
    [string]$PrivatePatternFile
)

$ErrorActionPreference = 'Stop'
if (-not $Repository) {
    $Repository = Split-Path -Parent $PSScriptRoot
}
$repositoryPath = (Resolve-Path -LiteralPath $Repository).Path
$errors = [System.Collections.Generic.List[string]]::new()

$relativeFiles = @(& git -C $repositoryPath ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) {
    throw 'Git could not enumerate the public tree.'
}

$forbiddenPaths = @(
    @{ Name = 'nested Git configuration'; Regex = '(?i)^\.gitmodules$' },
    @{ Name = 'private work material'; Regex = '(?i)(^|/)(private|private-records|construction|construction-logs|backups|artifacts|crash-reports|evidence|logs?)(/|$)' },
    @{ Name = 'private pattern file'; Regex = '(?i)(^|/)(private|sensitive)[-_].*\.(json|txt|yml|yaml)$' },
    @{ Name = 'retained build transcript'; Regex = '(?i)(^|/)(build[-_]?(final|log)|console[-_]?output)\.(txt|log)$' }
)
foreach ($relative in $relativeFiles) {
    foreach ($pattern in $forbiddenPaths) {
        if ($relative -match $pattern.Regex) {
            $errors.Add("$($pattern.Name) entered the repository: $relative")
        }
    }
}

$allowedBinaryFiles = @('gradle/wrapper/gradle-wrapper.jar')
$binaryExtensions = @('.jar', '.zip', '.l4z', '.dll', '.so', '.dylib', '.exe', '.class', '.7z', '.rar', '.gz')
foreach ($relative in $relativeFiles) {
    $extension = [System.IO.Path]::GetExtension($relative).ToLowerInvariant()
    if ($extension -in $binaryExtensions -and $relative -notin $allowedBinaryFiles) {
        $errors.Add("Unexpected binary or third-party archive: $relative")
    }
}

$contentPatterns = [System.Collections.Generic.List[object]]::new()
foreach ($pattern in @(
    @{ Name = 'absolute Windows path'; Regex = '(?i)(?<![A-Za-z0-9])[A-Z]:\\[^\r\n"''<>|]+' },
    @{ Name = 'absolute user-home path'; Regex = '(?i)(?<![A-Za-z0-9])/(Users|home)/[A-Za-z0-9._-]+(/|$)' },
    @{ Name = 'local file URI'; Regex = '(?i)\bfile:/+([A-Z]:/|Users/|home/)' }
)) {
    $contentPatterns.Add($pattern)
}

if (-not $PrivatePatternFile) {
    $PrivatePatternFile = $env:SIMULATED_PRIVATE_PATTERN_FILE
}
if ($PrivatePatternFile) {
    $resolvedPatternFile = (Resolve-Path -LiteralPath $PrivatePatternFile).Path
    $repositoryPrefix = $repositoryPath.TrimEnd('\') + '\'
    if ($resolvedPatternFile.StartsWith($repositoryPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'The private pattern file must be stored outside the repository.'
    }
    $document = Get-Content -LiteralPath $resolvedPatternFile -Raw | ConvertFrom-Json
    if ($document.version -ne 1 -or -not $document.patterns) {
        throw 'The private pattern file has an unsupported schema.'
    }
    $index = 0
    foreach ($privatePattern in $document.patterns) {
        $index++
        if (-not $privatePattern.regex) {
            throw "Private pattern $index has no regex."
        }
        [void][regex]::new([string]$privatePattern.regex)
        $contentPatterns.Add(@{ Name = "private identifier $index"; Regex = [string]$privatePattern.regex })
    }
}

$textExtensions = @('.java', '.groovy', '.gradle', '.kts', '.properties', '.json', '.toml', '.xml', '.md', '.txt', '.ps1', '.cmd', '.yml', '.yaml', '.rs', '.cs', '.glsl', '.patch', '.diff')
foreach ($relative in $relativeFiles) {
    $path = Join-Path $repositoryPath $relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        continue
    }
    $file = Get-Item -LiteralPath $path
    if ($file.Extension.ToLowerInvariant() -notin $textExtensions -or $file.Length -gt 5MB) {
        continue
    }
    $content = [System.IO.File]::ReadAllText($file.FullName)
    foreach ($pattern in $contentPatterns) {
        if ($content -match $pattern.Regex) {
            $errors.Add("$($pattern.Name) found in $relative")
        }
    }
}

foreach ($required in @('LICENSE.md', 'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar')) {
    if ($required -notin $relativeFiles) {
        $errors.Add("Required public build or license file is missing: $required")
    }
}

if ($errors.Count -gt 0) {
    Write-Error ("Public-tree audit failed:`n - " + ($errors -join "`n - "))
}

Write-Host "Public-tree audit passed: $($relativeFiles.Count) files checked."
