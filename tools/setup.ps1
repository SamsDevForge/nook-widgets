$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Path '.tooling' -Force | Out-Null
$downloads = @(
    @{File='jdk.zip'; Url='https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip'; Destination='jdk'},
    @{File='platform.zip'; Url='https://dl.google.com/android/repository/platform-35_r02.zip'; Destination='platform'},
    @{File='build-tools.zip'; Url='https://dl.google.com/android/repository/build-tools_r35_windows.zip'; Destination='build-tools'}
)
foreach ($download in $downloads) {
    $destination = Join-Path '.tooling' $download.Destination
    if (-not (Test-Path -LiteralPath $destination)) {
        Write-Output ('Downloading ' + $download.File)
        & curl.exe -L --fail --show-error --retry 2 -o (Join-Path '.tooling' $download.File) $download.Url
        if ($LASTEXITCODE -ne 0) { throw ('Download failed: ' + $download.File) }
        Expand-Archive -LiteralPath (Join-Path '.tooling' $download.File) -DestinationPath $destination -Force
    }
}
Write-Output 'Portable build tools ready. Run tools/build.ps1.'
