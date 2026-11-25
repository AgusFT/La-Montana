# Servicio local de La Montaña. No ofrece acceso remoto al sistema operativo.
$ErrorActionPreference='Stop'
$config=Get-Content (Join-Path $PSScriptRoot 'servicio.json') -Raw | ConvertFrom-Json
$distro='LaMontana-Ubuntu'
function Wsl([string[]]$Argumentos){
 $salida=& wsl.exe -d $distro -u root --exec @Argumentos
 if($LASTEXITCODE -ne 0){throw 'Falló una operación del servicio WSL.'}
 return $salida
}
$mantener=Start-Process wsl.exe -ArgumentList '-d LaMontana-Ubuntu -u root --exec sleep infinity' -PassThru -WindowStyle Hidden
try{
 Wsl @('systemctl','start','docker') | Out-Null
 $raiz=(Wsl @('wslpath','-u',$config.Raiz) | Out-String).Trim()
 $estado=Get-Content (Join-Path $config.Raiz 'instalacion.properties') -Raw | ConvertFrom-StringData
 if($estado['estado'] -in @('LISTA','PUBLICACION_PENDIENTE','VERIFICACION_PUBLICA_PENDIENTE')){
  Wsl @('docker','compose','--project-name',$config.Proyecto,'--project-directory',$raiz,'--env-file',"$raiz/.env",'-f',"$raiz/compose.yaml",'--profile','standard','up','-d','--wait','--wait-timeout','600','--pull','never') | Out-Null
  $boot=(Get-CimInstance Win32_OperatingSystem).LastBootUpTime.ToUniversalTime().Ticks
  [IO.File]::WriteAllText((Join-Path $config.Raiz 'arranque.properties'),("boot.id=$boot`n"))
 }
 $anterior=''
 while(-not $mantener.HasExited){
  $ip=((Wsl @('hostname','-I') | Out-String).Trim() -split '\s+')[0]
  if($ip -notmatch '^\d+\.\d+\.\d+\.\d+$'){throw 'IP WSL no válida.'}
  if($ip -ne $anterior){
   foreach($p in $config.Puertos){
    $clave="$($config.Bind)/$p"
    $marca="HKLM:\SOFTWARE\YG-Soluciones\LaMontana\Red\$($config.Proyecto)"
    $propio=Get-ItemPropertyValue $marca -Name $clave -ErrorAction Stop
    $previo=Get-ItemPropertyValue 'HKLM:\SYSTEM\CurrentControlSet\Services\PortProxy\v4tov4\tcp' -Name $clave -ErrorAction Stop
    if($previo -ne $propio){throw 'El reenvío de red cambió externamente; se requiere revisión presencial.'}
    & netsh.exe interface portproxy add v4tov4 "listenaddress=$($config.Bind)" "listenport=$p" "connectaddress=$ip" "connectport=$p" | Out-Null
    if($LASTEXITCODE -ne 0){throw 'No se pudo actualizar el reenvío de red.'}
    Set-ItemProperty $marca -Name $clave -Value "$ip/$p"
   }
   $anterior=$ip
  }
  Start-Sleep -Seconds 20
 }
 throw 'WSL finalizó; el Programador de tareas volverá a iniciar el servicio.'
}finally{if(-not $mantener.HasExited){$mantener.Kill()}}
