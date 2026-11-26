param([string]$Device = 'emulator-5556',[switch]$LibraryOnly,[switch]$StockOnly,[switch]$SourceOnly,[switch]$MediaOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if ($Device -notmatch '^emulator-\d+$') { throw 'These integration tests are intended for an emulator, not your personal phone.' }
$toolRoot = Join-Path $projectRoot '.tooling'
$buildRoot = Join-Path $projectRoot 'app/build/device-tests'
$androidTools = Join-Path $toolRoot 'build-tools/android-15'
$platform = Join-Path $toolRoot 'platform/android-35/android.jar'
$jdk = (Get-ChildItem (Join-Path $toolRoot 'jdk') -Directory | Select-Object -First 1).FullName
$java = Join-Path $jdk 'bin/java.exe'
$adb = Join-Path $toolRoot 'platform-tools/adb.exe'
$env:ANDROID_USER_HOME = Join-Path $toolRoot 'android-user'
function Run-Tool([string]$Executable, [string[]]$Arguments) { & $Executable @Arguments; if ($LASTEXITCODE -ne 0) { throw "Tool failed: $Executable" } }
New-Item -ItemType Directory -Force -Path $buildRoot,"$buildRoot/classes","$buildRoot/dex" | Out-Null
$manifest = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.nook.widgets.test"><uses-sdk android:minSdkVersion="28" android:targetSdkVersion="35"/><instrumentation android:name="com.nook.widgets.test.NookSmokeInstrumentation" android:targetPackage="com.nook.widgets"/><application android:label="Nook device checks" android:debuggable="true"/></manifest>'
[IO.File]::WriteAllText("$buildRoot/AndroidManifest.xml",$manifest,[Text.UTF8Encoding]::new($false))
Run-Tool (Join-Path $androidTools 'aapt2.exe') @('link','--manifest',"$buildRoot/AndroidManifest.xml",'-I',$platform,'-o',"$buildRoot/tests.apk")
Run-Tool (Join-Path $jdk 'bin/javac.exe') @('-encoding','UTF-8','-source','17','-target','17','-classpath',($platform+';'+"$projectRoot/app/build/portable/classes.jar"),'-d',"$buildRoot/classes",'app/src/androidTest/java/com/nook/widgets/test/NookSmokeInstrumentation.java','app/src/androidTest/java/com/nook/widgets/test/LibrarySmoke.java','app/src/androidTest/java/com/nook/widgets/test/ReplicaSmoke.java','app/src/androidTest/java/com/nook/widgets/test/StockSmoke.java','app/src/androidTest/java/com/nook/widgets/test/MediaSmoke.java')
Run-Tool (Join-Path $jdk 'bin/jar.exe') @('cf',"$buildRoot/tests.jar",'-C',"$buildRoot/classes",'.')
Run-Tool $java @('-cp',"$androidTools/lib/d8.jar",'com.android.tools.r8.D8','--min-api','28','--lib',$platform,'--classpath',"$projectRoot/app/build/portable/classes.jar",'--output',"$buildRoot/dex","$buildRoot/tests.jar")
Run-Tool (Join-Path $jdk 'bin/jar.exe') @('uf',"$buildRoot/tests.apk",'-C',"$buildRoot/dex",'classes.dex')
Run-Tool $java @('-jar',"$androidTools/lib/apksigner.jar",'sign','--ks',"$toolRoot/nook-debug.jks",'--ks-pass','pass:android','--key-pass','pass:android','--out',"$buildRoot/tests-signed.apk","$buildRoot/tests.apk")
Run-Tool $adb @('-s',$Device,'install','-r','dist/Nook-debug.apk')
Run-Tool $adb @('-s',$Device,'install','-r',"$buildRoot/tests-signed.apk")
Run-Tool $adb @('-s',$Device,'shell','pm','grant','com.nook.widgets','android.permission.POST_NOTIFICATIONS')
Run-Tool $adb @('-s',$Device,'shell','appops','set','com.nook.widgets','SCHEDULE_EXACT_ALARM','allow')
Run-Tool $adb @('-s',$Device,'shell','appwidget','grantbind','--package','com.nook.widgets','--user','0')
$instrumentArgs=@('-s',$Device,'shell','am','instrument','-w');if($LibraryOnly){$instrumentArgs+=@('-e','libraryOnly','true')};if($StockOnly){$instrumentArgs+=@('-e','stockOnly','true')};if($SourceOnly){$instrumentArgs+=@('-e','sourceOnly','true')};if($MediaOnly){$instrumentArgs+=@('-e','mediaOnly','true')};$instrumentArgs+='com.nook.widgets.test/com.nook.widgets.test.NookSmokeInstrumentation'
$testOutput = & $adb @instrumentArgs
$testOutput | Write-Output
& $adb -s $Device shell appwidget revokebind --package com.nook.widgets --user 0
if ($LASTEXITCODE -ne 0 -or ($testOutput -join "`n") -notmatch 'PASS: \d+ checks') { throw 'Android integration checks failed.' }
