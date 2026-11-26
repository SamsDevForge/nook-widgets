$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolRoot = Join-Path $projectRoot '.tooling'
$avdRoot = Join-Path $toolRoot 'avd'
$avdPath = Join-Path $avdRoot 'NookTest.avd'
New-Item -ItemType Directory -Force -Path $avdPath,(Join-Path $toolRoot 'android-user') | Out-Null
$env:ANDROID_AVD_HOME = $avdRoot
$env:ANDROID_USER_HOME = Join-Path $toolRoot 'android-user'
$env:ANDROID_EMULATOR_HOME = Join-Path $toolRoot 'android-user'
$env:ANDROID_PREFS_ROOT = $toolRoot
$env:ANDROID_SDK_ROOT = $toolRoot
$config = @"
AvdId=NookTest
avd.ini.displayname=Nook Test
avd.ini.encoding=UTF-8
abi.type=x86_64
disk.dataPartition.size=2G
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.battery=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.keyboard=yes
hw.lcd.density=420
hw.lcd.height=2340
hw.lcd.width=1080
hw.mainKeys=no
hw.ramSize=2048
hw.initialOrientation=Portrait
image.sysdir.1=$toolRoot\system-image\x86_64\
showDeviceFrame=no
tag.display=Default Android System Image
tag.id=default
vm.heapSize=256
"@
[IO.File]::WriteAllText((Join-Path $avdPath 'config.ini'),$config,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $avdRoot 'NookTest.ini'),"avd.ini.encoding=UTF-8`npath=$avdPath`ntarget=android-35`n",[Text.UTF8Encoding]::new($false))
$process = Start-Process -FilePath (Join-Path $toolRoot 'emulator/emulator.exe') -ArgumentList @('-avd','NookTest','-port','5556','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader','-no-metrics','-accel','on') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $toolRoot 'emulator.log') -RedirectStandardError (Join-Path $toolRoot 'emulator-error.log')
Write-Output ('Started isolated Nook test emulator. Process: ' + $process.Id)
