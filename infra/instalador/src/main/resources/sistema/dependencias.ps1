param([ValidateSet('wsl','ubuntu','docker','iniciar','navegador')][string]$Accion,[string]$Recurso)
$ErrorActionPreference='Stop'
$distro='LaMontanaDemo-Ubuntu'
try {
  switch ($Accion) {
    'wsl' {
      $feature=Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
      if (Get-Command wsl.exe -ErrorAction SilentlyContinue) {
        & wsl.exe --version | Out-Null
        if ($LASTEXITCODE -eq 0) {
          if ($feature.RestartNeeded) {exit 3010}
          exit 0
        }
      }
      $p=Start-Process msiexec.exe -ArgumentList @('/i',('"'+$Recurso+'"'),'/passive','/norestart') -Wait -PassThru
      if ($p.ExitCode -notin @(0,3010)) {exit $p.ExitCode}
      if ($feature.RestartNeeded -or $p.ExitCode -eq 3010) {exit 3010}
    }
    'ubuntu' {
      $existing=(& wsl.exe --list --quiet | Out-String).Replace([string][char]0,'')
      if (($existing -split '\r?\n').Trim() -contains $distro) {throw 'La distribución ya existe; no se reemplaza.'}
      $dest=Join-Path $env:LOCALAPPDATA 'YG-Soluciones\LaMontanaDemo\WSL'
      if (Test-Path -LiteralPath $dest) {
        if (-not (Test-Path -LiteralPath $dest -PathType Container) -or
            ((Get-Item -LiteralPath $dest).Attributes -band [IO.FileAttributes]::ReparsePoint) -or
            (Get-ChildItem -LiteralPath $dest -Force | Select-Object -First 1)) {
          throw 'El destino WSL de la Demo debe ser una carpeta propia y vacía.'
        }
      } else {New-Item -ItemType Directory -Path $dest -Force | Out-Null}
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
