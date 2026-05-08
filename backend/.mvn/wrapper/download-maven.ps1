param(
    [Parameter(Mandatory = $true)]
    [string] $Version,
    [Parameter(Mandatory = $true)]
    [string] $TargetDir
)

$ErrorActionPreference = "Stop"
$archiveName = "apache-maven-$Version-bin.zip"
$archivePath = Join-Path $TargetDir $archiveName
$mavenHome = Join-Path $TargetDir "apache-maven-$Version"
$url = "https://archive.apache.org/dist/maven/maven-3/$Version/binaries/$archiveName"

New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null

if (-not (Test-Path $archivePath)) {
    Invoke-WebRequest -Uri $url -OutFile $archivePath
}

if (Test-Path $mavenHome) {
    Remove-Item $mavenHome -Recurse -Force
}

Expand-Archive -Path $archivePath -DestinationPath $TargetDir -Force
