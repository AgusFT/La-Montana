param([Parameter(Mandatory)][string]$Paquete,[Parameter(Mandatory)][string]$Salida)
$ErrorActionPreference='Stop'
$Paquete=(Resolve-Path $Paquete).Path
if(Test-Path $Salida){throw 'La salida ya existe; no se reemplaza.'}
$manifest=Get-Content (Join-Path $Paquete 'release.properties') -Raw | ConvertFrom-StringData
if($manifest['platform'] -ne 'windows-x64' -or $manifest['offline.complete'] -ne 'true'){throw 'Se requiere el paquete completo Windows.'}
$version=$manifest['installer.version'];if($version -notmatch '^\d+\.\d+\.\d+$'){throw 'Versión inválida.'}
if(-not $env:JAVA_HOME){throw 'Indicar JAVA_HOME con JDK 17.'}
# WiX 3.14 debe estar en PATH en la máquina de construcción; no es una dependencia del cliente.
Get-Command candle.exe,light.exe -ErrorAction Stop | Out-Null
$build=Join-Path $PSScriptRoot 'target';New-Item -ItemType Directory "$build\classes" -Force | Out-Null
$sources=Get-ChildItem "$PSScriptRoot\src\main\java" -Filter *.java -Recurse | ForEach-Object FullName
& "$env:JAVA_HOME\bin\javac.exe" -J-Xmx256m --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d "$build\classes" $sources
if($LASTEXITCODE -ne 0){throw 'Falló javac.'}
Copy-Item "$PSScriptRoot\src\main\resources\*" "$build\classes" -Recurse -Force
& "$env:JAVA_HOME\bin\jar.exe" -J-Xmx128m --create --file "$build\instalador.jar" --main-class ar.com.lamontana.instalador.Main -C "$build\classes" .
if($LASTEXITCODE -ne 0){throw 'Falló jar.'}
$temp=Join-Path ([IO.Path]::GetTempPath()) ('lm-package-'+[Guid]::NewGuid())
try{
 New-Item -ItemType Directory "$temp\entrada" -Force | Out-Null
 Copy-Item "$build\instalador.jar" "$temp\entrada"
 Copy-Item $Paquete "$temp\entrada\paquete" -Recurse
 & "$env:JAVA_HOME\bin\jpackage.exe" -J-Xmx512m --type exe --name LaMontanaInstalador --app-version $version --vendor 'YG Soluciones' --description 'Instalación y mantenimiento de La Montaña' --input "$temp\entrada" --main-jar instalador.jar --main-class ar.com.lamontana.instalador.Main --java-options "-Xmx256m" --add-modules java.desktop,java.net.http,jdk.httpserver,jdk.crypto.ec --dest $Salida --win-per-user-install --win-dir-chooser --win-menu --win-shortcut --win-upgrade-uuid '770ca61f-4a85-4ff4-90d3-bd2511748818' --license-file "$PSScriptRoot\AVISOS-TERCEROS.txt"
 if($LASTEXITCODE -ne 0){throw 'Falló jpackage.'}
 Get-ChildItem $Salida -Filter *.exe | ForEach-Object {
  if($_.Length -ge 2GB){throw 'El ejecutable supera 2 GiB: revisar distribución antes de publicar.'}
  $hash=(Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
  [IO.File]::WriteAllText(($_.FullName+'.sha256'),($hash+'  '+$_.Name+"`n"))
 }
}finally{if(Test-Path $temp){Remove-Item $temp -Recurse -Force}}
