param([ValidateSet('arranque','red')][string]$Accion,[string]$Raiz,[string]$Proyecto,[string]$Bind,[int]$Puerto,[string]$Usuario,[string]$Trabajador)
$ErrorActionPreference='Stop'
if($Proyecto -notmatch '^lamontana-[a-f0-9]{12}$' -or $Puerto -lt 1024 -or $Puerto -gt 65535){throw 'Identidad o puerto inválido.'}
if(-not (Test-Path (Join-Path $Raiz 'instalacion.properties'))){throw 'Falta la instalación.'}
if($Bind -notmatch '^\d+\.\d+\.\d+\.\d+$'){throw 'IP inválida.'}
$actual=[Security.Principal.WindowsIdentity]::GetCurrent().Name
if($actual -ne $Usuario){throw 'Ejecutá esta operación con la misma cuenta Windows que instaló Ubuntu, elevando sus permisos.'}
$distro='LaMontana-Ubuntu'
$modo=(& wsl.exe -d $distro -u root --exec wslinfo --networking-mode | Out-String).Trim()
if($LASTEXITCODE -ne 0 -or $modo -notin @('nat','virtioproxy')){throw 'Esta ruta requiere la red NAT de WSL. No se modifica la red global de otras distribuciones.'}
$ip=((& wsl.exe -d $distro -u root --exec hostname -I | Out-String).Trim() -split '\s+')[0]
if($LASTEXITCODE -ne 0 -or $ip -notmatch '^\d+\.\d+\.\d+\.\d+$'){throw 'No se pudo identificar la IP de Ubuntu.'}
$puertos=if($Bind -eq '0.0.0.0'){@(80,443)}else{@($Puerto)}
$registro='HKLM:\SYSTEM\CurrentControlSet\Services\PortProxy\v4tov4\tcp'
$marca="HKLM:\SOFTWARE\YG-Soluciones\LaMontana\Red\$Proyecto"
function DestinoActual([string]$Clave){
 if(Test-Path $registro){return (Get-ItemPropertyValue $registro -Name $Clave -ErrorAction SilentlyContinue)}
 return $null
}
# No apropiarse de reenvíos de otra aplicación, aunque apunten a la misma IP.
foreach($p in $puertos){
 $clave="$Bind/$p";$previo=DestinoActual $clave
 $propio=if(Test-Path $marca){Get-ItemPropertyValue $marca -Name $clave -ErrorAction SilentlyContinue}else{$null}
 if($previo -and $previo -ne $propio){throw "El reenvío $clave ya pertenece a otra configuración. Revisalo antes de continuar."}
 if(-not $previo){
  $ocupado=Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue | Where-Object {$_.LocalAddress -in @($Bind,'0.0.0.0','::') -or $Bind -eq '0.0.0.0'}
  if($ocupado){throw "El puerto $p está ocupado por otro servicio de Windows."}
 }
}
if($Accion -eq 'red'){
 New-Item $marca -Force | Out-Null
 Set-Service iphlpsvc -StartupType Automatic;Start-Service iphlpsvc
 foreach($p in $puertos){
  & netsh.exe interface portproxy add v4tov4 "listenaddress=$Bind" "listenport=$p" "connectaddress=$ip" "connectport=$p"
  if($LASTEXITCODE -ne 0){throw 'No se pudo configurar el reenvío de puerto.'}
  New-ItemProperty $marca -Name "$Bind/$p" -Value "$ip/$p" -PropertyType String -Force | Out-Null
  $nombre="$Proyecto-$p"
  if(Get-NetFirewallRule -Name $nombre -ErrorAction SilentlyContinue){Remove-NetFirewallRule -Name $nombre}
  if($Bind -eq '0.0.0.0'){New-NetFirewallRule -Name $nombre -DisplayName "La Montana HTTPS $p" -Direction Inbound -Action Allow -Protocol TCP -LocalPort $p | Out-Null}
  else{New-NetFirewallRule -Name $nombre -DisplayName "La Montana LAN $p" -Direction Inbound -Action Allow -Protocol TCP -LocalPort $p -LocalAddress $Bind -RemoteAddress LocalSubnet -Profile Private,Domain | Out-Null}
 }
}else{
 if($Bind -ne '0.0.0.0'){throw 'El arranque automático corresponde a Standard.'}
 if(-not (Test-Path $marca)){throw 'Configurá primero la red de esta instalación.'}
 $cred=Get-Credential -UserName $Usuario -Message 'Windows guardará esta credencial en el Programador de tareas para iniciar La Montaña antes del inicio de sesión.'
 if(-not $cred -or $cred.UserName -ne $Usuario){throw 'Se canceló o se indicó otra cuenta Windows.'}
 $base=Join-Path $env:ProgramData "LaMontana\Servicios\$Proyecto"
 New-Item -ItemType Directory $base -Force | Out-Null
 & icacls.exe $base /inheritance:r /grant:r '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' ($Usuario+':(OI)(CI)RX') | Out-Null
 if($LASTEXITCODE -ne 0){throw 'No se pudieron restringir los permisos del servicio.'}
 Copy-Item $Trabajador (Join-Path $base 'trabajador.ps1') -Force
 @{Raiz=$Raiz;Proyecto=$Proyecto;Bind=$Bind;Puertos=$puertos} | ConvertTo-Json | Set-Content (Join-Path $base 'servicio.json') -Encoding UTF8
 $action=New-ScheduledTaskAction -Execute "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -Argument ('-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File "'+$base+'\trabajador.ps1"')
 $settings=New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
 Register-ScheduledTask -TaskName $Proyecto -Action $action -Trigger (New-ScheduledTaskTrigger -AtStartup) -Settings $settings -RunLevel Highest -User $Usuario -Password $cred.GetNetworkCredential().Password -Force | Out-Null
 $cred=$null
}
