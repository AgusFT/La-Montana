param([Parameter(Mandatory)][string]$Paquete,[Parameter(Mandatory)][string]$Salida)
$ErrorActionPreference='Stop'
& python "$PSScriptRoot\nativo\crear.py" $Paquete $Salida
exit $LASTEXITCODE
