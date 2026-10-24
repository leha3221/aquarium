$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$targetDirectory = Join-Path $projectRoot 'target'
$mavenRepository = Join-Path $targetDirectory 'maven-repository'
$settingsPath = Join-Path $targetDirectory 'maven-settings.xml'

New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
$repositoryXml = [System.Security.SecurityElement]::Escape($mavenRepository.Replace('\', '/'))
$settingsXml = "<settings xmlns=`"http://maven.apache.org/SETTINGS/1.2.0`"><localRepository>$repositoryXml</localRepository></settings>"
Set-Content -LiteralPath $settingsPath -Value $settingsXml -Encoding utf8

Push-Location $projectRoot
try {
    & mvn -s $settingsPath javafx:run
    $mavenExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $mavenExitCode
