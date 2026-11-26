param([Parameter(Mandatory)][string]$Paquete,[Parameter(Mandatory)][string]$Salida)
$ErrorActionPreference='Stop'
$Paquete=(Resolve-Path -LiteralPath $Paquete).Path
if(Test-Path -LiteralPath $Salida){throw 'La salida ya existe; no se reemplaza.'}
$manifest=Get-Content -LiteralPath (Join-Path $Paquete 'release.properties') -Raw | ConvertFrom-StringData
if($manifest['edition'] -cne 'DEMO' -or $manifest['platform'] -ne 'windows-x64' -or $manifest['offline.complete'] -ne 'true'){throw 'Se requiere el paquete completo de La Montaña Demo para Windows.'}
$version=$manifest['installer.version'];if($version -notmatch '^\d+\.\d+\.\d+$'){throw 'Versión inválida.'}
if(-not $env:JAVA_HOME){throw 'Indicar JAVA_HOME con JDK 17.'}
Get-Command candle.exe,light.exe -ErrorAction Stop | Out-Null

# Copiar únicamente la carga aprobada. Otras carpetas o archivos de la máquina de construcción son ajenos al instalador.
$locks=Get-Content -LiteralPath "$PSScriptRoot\dependencias\versiones.properties" -Raw | ConvertFrom-StringData
$archivos=@('images.tar.gz','AVISOS-TERCEROS.txt','docker-descargas.properties')
foreach($dep in @('docker','navegador','ubuntu','wsl')){
 $key="dep.windows.$dep"; $nombre=$locks["$key.file"]
 if($manifest["$key.file"] -cne $nombre -or $manifest["sha256.$nombre"] -cne $locks["$key.sha256"]){throw "Dependencia de Windows incorrecta: $dep."}
 $archivos+=$nombre
}
if(Compare-Object ($archivos | Sort-Object) ($manifest['files'].Split(',') | Sort-Object)){throw 'El contenido declarado difiere de la carga de la Demo.'}

$temp=Join-Path ([IO.Path]::GetTempPath()) ('lamontana-demo-'+[Guid]::NewGuid())
try{
 $entrada=Join-Path $temp 'entrada';$carga=Join-Path $entrada 'paquete';$clases=Join-Path $temp 'classes'
 New-Item -ItemType Directory $carga,$clases -Force | Out-Null
 foreach($archivo in $archivos){
  $destino=Join-Path $carga $archivo
  Copy-Item -LiteralPath (Join-Path $Paquete $archivo) -Destination $destino
  $esperado=$manifest["sha256.$archivo"]
  if($esperado -notmatch '^[0-9a-f]{64}$' -or (Get-FileHash -LiteralPath $destino -Algorithm SHA256).Hash -ne $esperado){throw "SHA-256 incorrecto: $archivo."}
 }
 Copy-Item -LiteralPath (Join-Path $Paquete 'release.properties') -Destination $carga
 $sources=Get-ChildItem "$PSScriptRoot\src\main\java" -Filter *.java -Recurse | ForEach-Object FullName
 & "$env:JAVA_HOME\bin\javac.exe" -J-Xmx256m --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d $clases $sources
 if($LASTEXITCODE -ne 0){throw 'Falló javac.'}
 Copy-Item "$PSScriptRoot\src\main\resources\*" $clases -Recurse -Force
 & "$env:JAVA_HOME\bin\jar.exe" -J-Xmx128m --create --file "$entrada\instalador.jar" --main-class ar.com.lamontana.instalador.Main -C $clases .
 if($LASTEXITCODE -ne 0){throw 'Falló jar.'}
 $mantenimiento=Join-Path $temp 'mantenimiento.properties'
 [IO.File]::WriteAllText($mantenimiento,"arguments=--mantenimiento`n")
 # Personalizar la plantilla de este JDK conserva los identificadores y las reglas de actualización de jpackage.
 $jdk=Join-Path $temp 'jdk';$wix=Join-Path $temp 'wix'
 & "$env:JAVA_HOME\bin\jmod.exe" extract --dir $jdk "$env:JAVA_HOME\jmods\jdk.jpackage.jmod"
 if($LASTEXITCODE -ne 0){throw 'No se pudo leer la plantilla de Windows Installer del JDK.'}
 $origen=Join-Path $jdk 'classes\jdk\jpackage\internal\resources\main.wxs'
 $plantilla=[IO.File]::ReadAllText($origen)
 if([regex]::Matches($plantilla,'<InstallExecuteSequence>').Count -ne 1){throw 'La plantilla WiX del JDK carece de la secuencia de instalación esperada.'}
 $retiro=@'
<CustomAction Id="RetirarServiciosLaMontanaDemo" Directory="INSTALLDIR"
      ExeCommand='&quot;[INSTALLDIR]LaMontanaDemo.exe&quot; --desinstalar-windows'
      Execute="deferred" Impersonate="yes" Return="check" />
    <UI>
      <ProgressText Action="RetirarServiciosLaMontanaDemo">Retirando los servicios de La Montaña Demo. Si falla, consultá [LocalAppDataFolder]YG-Soluciones\LaMontanaDemo\desinstalacion.log.</ProgressText>
    </UI>
    <InstallExecuteSequence>
      <Custom Action="RetirarServiciosLaMontanaDemo" Before="RemoveFiles">Installed AND REMOVE~="ALL" AND NOT UPGRADINGPRODUCTCODE AND NOT REINSTALL</Custom>
'@
 New-Item -ItemType Directory $wix | Out-Null
 [IO.File]::WriteAllText((Join-Path $wix 'main.wxs'),$plantilla.Replace('<InstallExecuteSequence>',$retiro))
 & "$env:JAVA_HOME\bin\jpackage.exe" -J-Xmx512m --verbose --type exe --resource-dir $wix --name LaMontanaDemo --app-version $version --vendor 'YG Soluciones' --description 'La Montaña Demo para Windows' --input $entrada --main-jar instalador.jar --main-class ar.com.lamontana.instalador.Main --add-launcher "LaMontanaDemo-Mantenimiento=$mantenimiento" --java-options "-Xmx256m" --add-modules java.desktop,java.net.http,jdk.httpserver,jdk.crypto.ec --dest $Salida --win-per-user-install --install-dir 'YG Soluciones\LaMontanaDemo' --win-menu --win-menu-group 'YG Soluciones' --win-shortcut --win-upgrade-uuid '07c34905-a7dd-4ca6-8c01-335a0bb17804' --license-file "$carga\AVISOS-TERCEROS.txt"
 if($LASTEXITCODE -ne 0){throw 'Falló jpackage.'}
 $exe=Get-Item -LiteralPath (Join-Path $Salida "LaMontanaDemo-$version.exe")
 if($exe.Length -ge 2GB){throw 'El ejecutable supera 2 GiB: revisar distribución antes de publicar.'}
 $hash=(Get-FileHash -LiteralPath $exe.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
 [IO.File]::WriteAllText(($exe.FullName+'.sha256'),($hash+'  '+$exe.Name+"`n"))
}finally{if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp -Recurse -Force}}
