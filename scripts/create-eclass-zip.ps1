# Creates the final eClass ZIP only from real GitHub links, screenshot and test reports.
# Run from Windows PowerShell 5.1+ or PowerShell 7.
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9-]+$')]
    [string]$GitHubUsername,

    [Parameter()]
    [ValidatePattern('^[A-Za-z0-9_.-]+$')]
    [string]$Repository = 'citizen-registry-ci-cd',

    [Parameter(Mandatory = $true)]
    [string]$ScreenshotPath,

    [Parameter(Mandatory = $true)]
    [string]$UnitReportsPath,

    [Parameter(Mandatory = $true)]
    [string]$ApiReportsPath,

    [string]$OutputDirectory = '.\eclass-output'
)

$ErrorActionPreference = 'Stop'

foreach ($path in @($ScreenshotPath, $UnitReportsPath, $ApiReportsPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Path does not exist: $path"
    }
}

$screenshot = Get-Item -LiteralPath $ScreenshotPath
if ($screenshot.PSIsContainer -or $screenshot.Extension -notin @('.png', '.jpg', '.jpeg')) {
    throw 'ScreenshotPath must point to a real .png/.jpg screenshot file.'
}

$unitXml = @(Get-ChildItem -LiteralPath $UnitReportsPath -Recurse -File -Filter 'TEST-*.xml')
$apiXml = @(Get-ChildItem -LiteralPath $ApiReportsPath -Recurse -File -Filter 'TEST-*.xml')
if ($unitXml.Count -eq 0) { throw 'No unit TEST-*.xml report was found.' }
if ($apiXml.Count -eq 0) { throw 'No API/integration TEST-*.xml report was found.' }

$output = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -Path $output -ItemType Directory -Force | Out-Null
$archive = Join-Path $output 'Emmanouil_Daramas.zip'
if (Test-Path -LiteralPath $archive) {
    throw "The output file already exists; move/rename it first: $archive"
}

$staging = Join-Path ([IO.Path]::GetTempPath()) ('citizen-eclass-' + [guid]::NewGuid().ToString('N'))
try {
    New-Item -Path $staging -ItemType Directory -Force | Out-Null
    $unitDir = Join-Path $staging 'unit-test-reports'
    $apiDir = Join-Path $staging 'api-integration-test-reports'
    New-Item -Path $unitDir, $apiDir -ItemType Directory -Force | Out-Null

    $links = @(
        "Repository URL: https://github.com/$GitHubUsername/$Repository",
        "GitHub Actions workflow URL: https://github.com/$GitHubUsername/$Repository/blob/develop/.github/workflows/ci.yml"
    )
    $links | Set-Content -Path (Join-Path $staging 'links.txt') -Encoding UTF8
    Copy-Item -LiteralPath $screenshot.FullName -Destination (Join-Path $staging ('pipeline-screenshot' + $screenshot.Extension))

    $reportSets = @(
        [pscustomobject]@{ Source = $UnitReportsPath; Destination = $unitDir },
        [pscustomobject]@{ Source = $ApiReportsPath; Destination = $apiDir }
    )
    foreach ($spec in $reportSets) {
        $source = $spec.Source
        $destination = $spec.Destination
        $files = Get-ChildItem -LiteralPath $source -Recurse -File | Where-Object { $_.Extension -in @('.xml', '.html', '.txt') }
        foreach ($file in $files) {
            $target = Join-Path $destination $file.Name
            if (Test-Path -LiteralPath $target) { throw "Duplicate report file name: $($file.Name)" }
            Copy-Item -LiteralPath $file.FullName -Destination $target
        }
    }

    Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $archive -CompressionLevel Optimal
    Write-Host "Final eClass ZIP created successfully: $archive" -ForegroundColor Green
    Write-Host "Unit XML reports: $($unitXml.Count); API/integration XML reports: $($apiXml.Count)"
}
finally {
    if (Test-Path -LiteralPath $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}
