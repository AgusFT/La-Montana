Target amd64-unicode
Unicode true
!include MUI2.nsh
!include LogicLib.nsh
Name "La Montaña Demo"
OutFile "${SALIDA}"
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa"
SetCompressor zlib
!define REGKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\LaMontanaDemoNativa"
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION Abrir
!define MUI_FINISHPAGE_RUN_TEXT "Abrir La Montaña Demo"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Spanish"
VIProductVersion "${VERSION}.0"
VIAddVersionKey /LANG=1034 "ProductName" "La Montaña Demo"
VIAddVersionKey /LANG=1034 "CompanyName" "YG Soluciones"
VIAddVersionKey /LANG=1034 "FileDescription" "Instalador de La Montaña Demo para Windows x64"
VIAddVersionKey /LANG=1034 "FileVersion" "${VERSION}"
VIAddVersionKey /LANG=1034 "LegalCopyright" "YG Soluciones"
!macro Detener
  ${If} ${FileExists} "$INSTDIR\app\panel.jar"
    ExecWait '"$INSTDIR\app\java\bin\javaw.exe" -jar "$INSTDIR\app\panel.jar" --detener' $0
    ${If} $0 != 0
      Abort "Cerrá la demo y volvé a intentar."
    ${EndIf}
  ${EndIf}
!macroend
!macro Exclusivo
  System::Call 'kernel32::CreateMutexW(p 0, i 0, w "Local\LaMontanaDemoNativaInstalacion") p.r0 ?e'
  Pop $1
  ${If} $0 == 0
  ${OrIf} $1 == 183
    Abort "Ya hay otra instalación de la demo en curso."
  ${EndIf}
!macroend
Function .onInit
  StrCpy $INSTDIR "$LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa"
  !insertmacro Exclusivo
FunctionEnd
Function un.onInit
  ${If} $INSTDIR != "$LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa"
    Abort "Ubicación de desinstalación incorrecta."
  ${EndIf}
  !insertmacro Exclusivo
FunctionEnd
Function Abrir
  ExecShell "open" "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk"
FunctionEnd
Section
  SetShellVarContext current
  SetOutPath "$INSTDIR"
  !insertmacro Detener
  ${If} ${FileExists} "$INSTDIR\anterior\panel.jar"
  ${AndIfNot} ${FileExists} "$INSTDIR\app\panel.jar"
    Rename "$INSTDIR\anterior" "$INSTDIR\app"
  ${EndIf}
  RMDir /r "$INSTDIR\nueva"
  CreateDirectory "$INSTDIR\nueva"
  InitPluginsDir
  SetOutPath "$PLUGINSDIR"
  SetCompress off
  File /oname=contenido.zip "${CONTENIDO}"
  DetailPrint "Instalando los componentes de Windows…"
  nsExec::ExecToLog '"$SYSDIR\tar.exe" -xf "$PLUGINSDIR\contenido.zip" -C "$INSTDIR\nueva"'
  Pop $0
  ${If} $0 != 0
    RMDir /r "$INSTDIR\nueva"
    Abort "Windows no pudo extraer la demo. Revisá el espacio disponible."
  ${EndIf}
  SetOutPath "$INSTDIR"
  RMDir /r "$INSTDIR\anterior"
  ClearErrors
  ${If} ${FileExists} "$INSTDIR\app\panel.jar"
    Rename "$INSTDIR\app" "$INSTDIR\anterior"
    IfErrors 0 +2
    Abort "No se pudo preparar la actualización. Cerrá la demo y volvé a intentar."
  ${EndIf}
  Rename "$INSTDIR\nueva" "$INSTDIR\app"
  ${If} ${Errors}
    Rename "$INSTDIR\anterior" "$INSTDIR\app"
    Abort "No se pudo activar la instalación."
  ${EndIf}
  RMDir /r "$INSTDIR\anterior"
  WriteUninstaller "$INSTDIR\Desinstalar.exe"
  CreateDirectory "$SMPROGRAMS\YG Soluciones"
  CreateShortCut "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk" "$INSTDIR\app\java\bin\javaw.exe" '-jar "$INSTDIR\app\panel.jar"'
  WriteRegStr HKCU "${REGKEY}" "DisplayName" "La Montaña Demo (Windows nativa)"
  WriteRegStr HKCU "${REGKEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "${REGKEY}" "Publisher" "YG Soluciones"
  WriteRegStr HKCU "${REGKEY}" "UninstallString" '"$INSTDIR\Desinstalar.exe"'
SectionEnd
Section "Uninstall"
  SetShellVarContext current
  !insertmacro Detener
  ClearErrors
  RMDir /r "$INSTDIR\app"
  ${If} ${Errors}
    Abort "Windows todavía tiene archivos de la demo abiertos. Cerrá la aplicación y volvé a intentar."
  ${EndIf}
  RMDir /r "$INSTDIR\nueva"
  RMDir /r "$INSTDIR\anterior"
  Delete "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk"
  RMDir "$SMPROGRAMS\YG Soluciones"
  DeleteRegKey HKCU "${REGKEY}"
  Delete "$INSTDIR\Desinstalar.exe"
  RMDir "$INSTDIR"
SectionEnd
