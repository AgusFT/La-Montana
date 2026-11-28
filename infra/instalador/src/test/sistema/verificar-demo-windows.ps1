# Ejecutar con el panel nativo abierto. Se descubre la URL a partir del proceso instalado.
param([string]$Raiz, [string]$Anterior)
$ErrorActionPreference = 'Stop'
$registro = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\LaMontanaDemoNativa' -ErrorAction SilentlyContinue
if (!$Raiz) {
    $programa = if ($registro.InstallLocation) { $registro.InstallLocation } else { "$env:LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa" }
    $Raiz = Join-Path $programa 'app'
}
$Raiz = (Resolve-Path -LiteralPath $Raiz).Path
$version = [IO.File]::ReadAllText((Join-Path $Raiz 'version.txt')).Trim()
if ([version]$version -ge [version]'0.1.7') {
    $icono = Join-Path $Raiz 'montana.ico'
    $cabecera = [IO.File]::ReadAllBytes($icono)
    if ($cabecera[2] -ne 1 -or $cabecera[4] -lt 5 -or $registro.DisplayIcon -ne $icono) { throw 'Ícono o registro de la aplicación incorrecto.' }
    $shell = New-Object -ComObject WScript.Shell
    $enlaces = @(Join-Path ([Environment]::GetFolderPath('Programs')) 'YG Soluciones\La Montaña Demo.lnk')
    if ($registro.AccesoEscritorio -eq 1) {
        $enlaces += Join-Path ([Environment]::GetFolderPath('DesktopDirectory')) 'La Montaña Demo.lnk'
    }
    foreach ($archivo in $enlaces) {
        if (!(Test-Path -LiteralPath $archivo)) { throw "Falta el acceso $archivo." }
        $enlace = $shell.CreateShortcut($archivo)
        if ($enlace.TargetPath -ne (Join-Path $Raiz 'java\bin\javaw.exe') -or ($enlace.IconLocation -split ',(?=\s*0$)')[0] -ne $icono) {
            throw "El destino o el ícono del acceso es incorrecto: $archivo"
        }
    }
}
$todos = @(Get-CimInstance Win32_Process)
$procesos = @($todos | Where-Object { $_.ExecutablePath -and $_.ExecutablePath.StartsWith($Raiz+'\', [StringComparison]::OrdinalIgnoreCase) })
# En rutas Unicode, Windows informa la letra temporal del binario de PostgreSQL.
$datos = Join-Path $env:LOCALAPPDATA 'YG-Soluciones\LaMontanaDemo\nativa'
$marcaPg = Join-Path $datos 'unidad-postgres-programa.txt'
$pidPg = Join-Path $datos 'postgres\postmaster.pid'
if ((Test-Path -LiteralPath $marcaPg) -and (Test-Path -LiteralPath $pidPg)) {
    $letra = (Get-Content -Raw -LiteralPath $marcaPg).Trim()
    if ($letra -cnotmatch '^[D-Z]:$') { throw 'Letra temporal de PostgreSQL inválida.' }
    $ejecutablePg = $letra + '\pg\bin\postgres.exe'
    $maestro = [int](Get-Content -TotalCount 1 -LiteralPath $pidPg)
    $servidores = @($todos | Where-Object { $_.ExecutablePath -eq $ejecutablePg })
    if ($maestro -in $servidores.ProcessId) { $procesos += $servidores }
    $pidAntivirus = [int](Get-Content -TotalCount 1 -LiteralPath (Join-Path $datos 'antivirus.pid'))
    $procesos += @($todos | Where-Object { $_.ExecutablePath -eq ($letra + '\clamav\clamd.exe') -and $_.ProcessId -eq $pidAntivirus })
}
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
[ordered]@{version=$version;modo=$modo;url=$url;inicio=$inicio;procesos=@($procesos.Name | Sort-Object -Unique);puertos=@($escuchas.LocalPort | Sort-Object -Unique)} | ConvertTo-Json
