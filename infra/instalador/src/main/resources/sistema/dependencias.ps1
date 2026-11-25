param([ValidateSet('wsl','ubuntu','docker','iniciar','navegador')][string]$Accion,[string]$Recurso)
$ErrorActionPreference='Stop'
$distro='LaMontana-Ubuntu'
try {
  switch ($Accion) {
    'wsl' {
      $feature=Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
      $p=Start-Process msiexec.exe -ArgumentList @('/i',('"'+$Recurso+'"'),'/passive','/norestart') -Wait -PassThru
      if ($p.ExitCode -notin @(0,3010)) {exit $p.ExitCode}
      if ($feature.RestartNeeded -or $p.ExitCode -eq 3010) {exit 3010}
    }
    'ubuntu' {
      $existing=(& wsl.exe --list --quiet | Out-String).Replace([string][char]0,'')
      if (($existing -split '\r?\n').Trim() -contains $distro) {throw 'La distribución ya existe; no se reemplaza.'}
      $dest=Join-Path $env:LOCALAPPDATA 'LaMontana\WSL'
      if (Test-Path $dest) {throw 'Existe un directorio WSL previo; se necesita revisión manual.'}
      New-Item -ItemType Directory -Path $dest | Out-Null
      & wsl.exe --import $distro $dest $Recurso --version 2
      if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
    }
    'docker' {
      $linux=(& wsl.exe -d $distro -u root --exec wslpath -u $Recurso | Out-String).Trim()
      if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
      # Reutilizar el auxiliar Ubuntu evita que PowerShell 5 quite las comillas de bash -c.
      $auxiliar=(& wsl.exe -d $distro -u root --exec wslpath -u (Join-Path $PSScriptRoot 'dependencias.sh') | Out-String).Trim()
      if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
      & wsl.exe -d $distro -u root --exec bash $auxiliar docker $linux
      if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
    }
    'iniciar' { & wsl.exe -d $distro -u root --exec systemctl start docker; if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE} }
    'navegador' { $p=Start-Process -FilePath $Recurso -Wait -PassThru; exit $p.ExitCode }
  }
  exit 0
} catch { Write-Error $_; exit 1 }
