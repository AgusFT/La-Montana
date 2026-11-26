# Ejecutar en Windows 11 tras instalar, reparar o reinstalar la Demo.
# La salida es JSON. Con -Anterior se coteja un resultado guardado en otra ejecucion.
param(
    [Parameter(Mandatory=$true)][string]$Raiz,
    [string]$Anterior
)
$ErrorActionPreference = 'Stop'
function WslDemo([string[]]$Argumentos) {
    $salida = & wsl.exe -d LaMontanaDemo-Ubuntu -u root --exec @Argumentos
    if ($LASTEXITCODE -ne 0) { throw "WSL no pudo ejecutar $($Argumentos[0])." }
    return $salida
}

$distros = (& wsl.exe --list --verbose | Out-String).Replace([string][char]0,'')
if ($LASTEXITCODE -ne 0 -or $distros -notmatch '(?m)^\s*\*?\s*LaMontanaDemo-Ubuntu\s+[^\r\n]+?\s+2\s*$') { throw 'Falta la distribucion WSL2 de la Demo.' }
$estado = @{}
foreach ($linea in (Get-Content -LiteralPath (Join-Path $Raiz 'instalacion.properties'))) {
    if ($linea -match '^(modo|proyecto|puerto)=(.+)$') { $estado[$Matches[1]] = $Matches[2] }
}
$proyecto = $estado.proyecto
if ($estado.modo -ne 'DEMO' -or $proyecto -notmatch '^lamontana-demo-[a-f0-9]{12}$') { throw 'Identidad de instalacion inesperada.' }
$base = 'http://127.0.0.1:' + [int]$estado.puerto
$vigencia = Invoke-RestMethod -Uri "$base/api/demo/vigencia" -TimeoutSec 20
if ($vigencia.estado -notin @('VIGENTE','VENCIDA')) { throw "Vigencia inesperada: $($vigencia.estado)." }
if ($vigencia.estado -eq 'VIGENTE' -and (Invoke-RestMethod -Uri "$base/api/sistema/entorno" -TimeoutSec 20).modo -ne 'DEMO') { throw 'El servidor no es la Demo.' }

$marca = Join-Path ([Environment]::GetFolderPath('CommonApplicationData')) 'YG-Soluciones\LaMontanaDemo\vigencia'
$fechaMarca = (Get-Content -LiteralPath (Join-Path $marca 'vigencia.txt') -TotalCount 1)
$inicio = [DateTimeOffset]$vigencia.inicio
$vence = [DateTimeOffset]$vigencia.venceEn
if ($inicio -ne [DateTimeOffset]$fechaMarca -or ($vence - $inicio) -ne [TimeSpan]::FromHours(360)) { throw 'El plazo no coincide con el registro del equipo.' }

$montaje = (WslDemo @('wslpath','-u',$marca) | Out-String).Trim()
$ids = @(WslDemo @('docker','ps','-q','--filter',"label=com.docker.compose.project=$proyecto"))
if ($ids.Count -ne 5) { throw "Se esperaban cinco servicios de la Demo, hay $($ids.Count)." }
$tieneMarca = $false
foreach ($id in $ids) {
    $volumenes = (WslDemo @('docker','inspect','--format','{{json .Mounts}}',$id) | Out-String) | ConvertFrom-Json
    foreach ($volumen in $volumenes) {
        if ($volumen.Type -eq 'volume' -and -not $volumen.Name.StartsWith("${proyecto}_")) { throw "Volumen ajeno: $($volumen.Name)." }
        if ($volumen.Destination -eq '/app/vigencia' -and $volumen.Type -eq 'bind' -and $volumen.Source -eq $montaje -and $volumen.RW) { $tieneMarca = $true }
    }
    foreach ($puerto in (WslDemo @('docker','port',$id))) {
        if ($puerto -notmatch ' -> 127\.0\.0\.1:\d+$') { throw "Puerto ajeno al acceso local: $puerto." }
    }
}
if (-not $tieneMarca) { throw 'Ningun servicio tiene montada la vigencia del equipo.' }
if ($Anterior) {
    $previo = Get-Content -Raw -LiteralPath $Anterior | ConvertFrom-Json
    if ($inicio -ne [DateTimeOffset]$previo.inicio -or $vence -ne [DateTimeOffset]$previo.venceEn) { throw 'El plazo cambio entre instalaciones.' }
}
[ordered]@{proyecto=$proyecto; servicios=$ids.Count; estado=$vigencia.estado; inicio=$inicio.ToUniversalTime().ToString('o'); venceEn=$vence.ToUniversalTime().ToString('o')} | ConvertTo-Json
