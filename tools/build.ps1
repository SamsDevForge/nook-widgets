param([switch]$SkipResources)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$toolRoot = Join-Path $projectRoot '.tooling'
$output = Join-Path $projectRoot 'dist'
$buildRoot = Join-Path $projectRoot 'app/build/portable'

function Run-Tool([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Tool failed ($LASTEXITCODE): $Executable" }
}

$jdk = Get-ChildItem (Join-Path $toolRoot 'jdk') -Directory | Select-Object -First 1
$platform = Join-Path $toolRoot 'platform/android-35/android.jar'
$androidTools = Join-Path $toolRoot 'build-tools/android-15'
if (-not $jdk -or -not (Test-Path -LiteralPath $platform) -or -not (Test-Path -LiteralPath $androidTools)) {
    throw 'Portable tools are missing. Run tools/setup.ps1, or open the project in Android Studio and build with Gradle.'
}
$java = Join-Path $jdk.FullName 'bin/java.exe'
$javac = Join-Path $jdk.FullName 'bin/javac.exe'
$jar = Join-Path $jdk.FullName 'bin/jar.exe'
$keytool = Join-Path $jdk.FullName 'bin/keytool.exe'
$aapt = Join-Path $androidTools 'aapt2.exe'

New-Item -ItemType Directory -Force -Path $output,$buildRoot,(Join-Path $buildRoot 'generated'),(Join-Path $buildRoot 'classes'),(Join-Path $buildRoot 'dex') | Out-Null
if (-not $SkipResources) { Run-Tool 'node' @('tools/generate-resources.mjs') }
Run-Tool $aapt @('compile','--dir','app/src/main/res','-o',"$buildRoot/resources.zip")
$manifestText = [IO.File]::ReadAllText((Join-Path $projectRoot 'app/src/main/AndroidManifest.xml')).Replace('<manifest ', '<manifest package="com.nook.widgets" ').Replace('<application ', '<application android:debuggable="true" ')
[IO.File]::WriteAllText("$buildRoot/AndroidManifest.xml", $manifestText, [Text.UTF8Encoding]::new($false))
Run-Tool $aapt @('link','--manifest',"$buildRoot/AndroidManifest.xml",'--min-sdk-version','28','--target-sdk-version','35','--version-code','12','--version-name','0.11.0','--auto-add-overlay','--java',"$buildRoot/generated",'-I',$platform,'-R',"$buildRoot/resources.zip",'-o',"$buildRoot/unsigned.apk")
$sourceFiles = @(Get-ChildItem 'app/src/main/java',"$buildRoot/generated" -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
# javac argument files avoid command-line limits and preserve workspace paths containing spaces.
$javacArgs = @('-encoding','UTF-8','-source','17','-target','17','-classpath',$platform,'-d',"$buildRoot/classes") + $sourceFiles
$argumentFile = Join-Path $buildRoot 'javac.args'
[IO.File]::WriteAllLines($argumentFile, ($javacArgs | ForEach-Object { '"' + $_.Replace('\','/') + '"' }), [Text.UTF8Encoding]::new($false))
Run-Tool $javac @("@$argumentFile")
Run-Tool $jar @('cf',"$buildRoot/classes.jar",'-C',"$buildRoot/classes",'.')
Run-Tool $java @('-cp',"$androidTools/lib/d8.jar",'com.android.tools.r8.D8','--release','--min-api','28','--lib',$platform,'--output',"$buildRoot/dex","$buildRoot/classes.jar")
Run-Tool $jar @('uf',"$buildRoot/unsigned.apk",'-C',"$buildRoot/dex",'classes.dex')
Run-Tool (Join-Path $androidTools 'zipalign.exe') @('-f','-p','4',"$buildRoot/unsigned.apk","$buildRoot/aligned.apk")
$keyStore = Join-Path $toolRoot 'nook-debug.jks'
if (-not (Test-Path -LiteralPath $keyStore)) {
    Run-Tool $keytool @('-genkeypair','-keystore',$keyStore,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Nook Development, O=Nook, C=IN')
}
Run-Tool $java @('-jar',"$androidTools/lib/apksigner.jar",'sign','--ks',$keyStore,'--ks-pass','pass:android','--key-pass','pass:android','--out',"$output/Nook-debug.apk","$buildRoot/aligned.apk")
Run-Tool $java @('-jar',"$androidTools/lib/apksigner.jar",'verify','--verbose',"$output/Nook-debug.apk")
Write-Output "Built: $output/Nook-debug.apk"
