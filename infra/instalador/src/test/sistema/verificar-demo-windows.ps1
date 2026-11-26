# Ejecutar con el panel nativo abierto. Se descubre la URL a partir del proceso instalado.
param([string]$Raiz = "$env:LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa\app", [string]$Anterior)
$ErrorActionPreference = 'Stop'
$Raiz = (Resolve-Path -LiteralPath $Raiz).Path
$procesos = @(Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -and $_.ExecutablePath.StartsWith($Raiz+'\', [StringComparison]::OrdinalIgnoreCase) })
foreach ($nombre in 'postgres.exe','clamd.exe','mailpit.exe','java.exe','node.exe') {
    if (!($procesos | Where-Object Name -eq $nombre)) { throw "Falta el proceso nativo $nombre." }
}
$escuchas = @(Get-NetTCPConnection -State Listen | Where-Object { $_.OwningProcess -in $procesos.ProcessId })
if (!$escuchas -or ($escuchas | Where-Object { $_.LocalAddress -notin '127.0.0.1','::1' })) { throw 'Las escuchas de la demo deben ser locales.' }
$node = @($procesos | Where-Object Name -eq 'node.exe')
$puerto = @($escuchas | Where-Object OwningProcess -in $node.ProcessId | Select-Object -ExpandProperty LocalPort -Unique)
if ($puerto.Count -ne 1) { throw 'No se identificó la aplicación web instalada.' }
$url = "http://127.0.0.1:$($puerto[0])"
$modo = (Invoke-RestMethod "$url/api/sistema/entorno" -TimeoutSec 8).modo
if ($modo -cne 'DEMO') { throw 'Identidad de edición incorrecta.' }
$inicio = [IO.File]::ReadAllLines((Join-Path $env:ProgramData 'YG-Soluciones\LaMontanaDemo\vigencia\vigencia.txt'))[0]
if ($Anterior -and (Get-Content -Raw -LiteralPath $Anterior | ConvertFrom-Json).inicio -ne $inicio) { throw 'Cambió el inicio de vigencia.' }
[ordered]@{version=[IO.File]::ReadAllText("$Raiz\version.txt").Trim();modo=$modo;url=$url;inicio=$inicio;procesos=@($procesos.Name | Sort-Object -Unique);puertos=@($escuchas.LocalPort | Sort-Object -Unique)} | ConvertTo-Json
