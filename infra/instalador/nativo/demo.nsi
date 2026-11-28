Target amd64-unicode
Unicode true
!include MUI2.nsh
!include LogicLib.nsh
!include Sections.nsh
!include StrFunc.nsh
${UnStrStr}
Name "La Montaña Demo"
OutFile "${SALIDA}"
RequestExecutionLevel user
!define RAIZ "$LOCALAPPDATA\Programs\YG Soluciones\LaMontanaDemoNativa"
!define REGKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\LaMontanaDemoNativa"
InstallDir "${RAIZ}"
SetCompressor zlib
Var Instalada
Var Inventario
Var EscritorioAnterior
!define MUI_ICON "montana.ico"
!define MUI_UNICON "montana.ico"
!define MUI_WELCOMEPAGE_TITLE "La Montaña Demo en tu PC"
!define MUI_WELCOMEPAGE_TEXT "La demo abre en tu navegador y funciona en este equipo. Java ejecuta las operaciones y Node.js muestra la interfaz. PostgreSQL guarda tus pedidos. ClamAV revisa los adjuntos y busca actualizaciones de firmas por Internet. Mailpit recibe los correos de prueba en un buzón local.$\r$\n$\r$\nSe instala para tu usuario de Windows. En el siguiente paso vas a ver dónde quedan el programa y sus datos."
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION Abrir
!define MUI_FINISHPAGE_RUN_TEXT "Abrir La Montaña Demo"
!insertmacro MUI_PAGE_WELCOME
!define MUI_DIRECTORYPAGE_TEXT_TOP "Elegí la carpeta del programa. Si ya instalaste la demo, se conservará su ubicación. Los pedidos, archivos y registros se guardan aparte en $LOCALAPPDATA\YG-Soluciones\LaMontanaDemo\nativa. La vigencia queda en %ProgramData%\YG-Soluciones\LaMontanaDemo\vigencia y se conserva al reinstalar."
!define MUI_PAGE_CUSTOMFUNCTION_SHOW MostrarDirectorio
!insertmacro MUI_PAGE_DIRECTORY
!define MUI_COMPONENTSPAGE_TEXT_TOP "El menú Inicio siempre incluye La Montaña Demo. También podés agregar un acceso en el escritorio. Ambos abren el mismo programa."
!insertmacro MUI_PAGE_COMPONENTS
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

!macro Fallo texto
  SetErrorLevel 1
  DetailPrint "${texto}"
  MessageBox MB_OK|MB_ICONSTOP "${texto}" /SD IDOK
  Abort
!macroend
!macro Detener nodo lista
  ${If} $Instalada != ""
    nsExec::ExecToStack '"${nodo}" "$PLUGINSDIR\archivos.mjs" validar "$INSTDIR" "${lista}"'
    Pop $0
    Pop $1
    ${If} $0 != 0
      !insertmacro Fallo "No se pudo comprobar la instalación existente. $1"
    ${EndIf}
  ${EndIf}
  ${If} $Instalada != ""
  ${AndIf} ${FileExists} "$INSTDIR\app\panel.jar"
    DetailPrint "Cerrando los servicios de la instalación existente…"
    ExecWait '"$INSTDIR\app\java\bin\javaw.exe" -jar "$INSTDIR\app\panel.jar" --detener' $0
    ${If} $0 != 0
      !insertmacro Fallo "Cerrá la demo y volvé a intentar."
    ${EndIf}
  ${EndIf}
!macroend
!macro Exclusivo
  System::Call 'kernel32::CreateMutexW(p 0, i 0, w "Local\LaMontanaDemoNativaInstalacion") p.r0 ?e'
  Pop $1
  ${If} $0 == 0
  ${OrIf} $1 == 183
    !insertmacro Fallo "Ya hay otra operación de instalación en curso."
  ${EndIf}
!macroend
Function MostrarDirectorio
  ${If} $Instalada != ""
    EnableWindow $mui.DirectoryPage.Directory 0
    EnableWindow $mui.DirectoryPage.BrowseButton 0
  ${EndIf}
FunctionEnd
Function Abrir
  ExecShell "open" "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk"
FunctionEnd

Section "Aplicación web y servicios locales" Programa
  SectionIn RO
  AddSize ${TAMANO_KIB}
  SetShellVarContext current
  InitPluginsDir
  SetOutPath "$PLUGINSDIR"
  SetCompress off
  File /oname=contenido.zip "${CONTENIDO}"
  SetCompress auto
  File archivos.mjs
  File /oname=actual.gz "${INVENTARIO}"
  File archivos-0.1.5.sha256.gz
  File archivos-0.1.6.sha256.gz
  File archivos-0.1.7.sha256.gz
  DetailPrint "Preparando los componentes de Windows…"
  nsExec::ExecToLog '"$SYSDIR\tar.exe" -xf "$PLUGINSDIR\contenido.zip" -C "$PLUGINSDIR" node/node.exe'
  Pop $0
  ${If} $0 != 0
    !insertmacro Fallo "Windows no pudo preparar el instalador. Revisá el espacio en la unidad temporal."
  ${EndIf}
  !insertmacro Detener "$PLUGINSDIR\node\node.exe" "$Inventario"
  nsExec::ExecToStack '"$PLUGINSDIR\node\node.exe" "$PLUGINSDIR\archivos.mjs" instalar "$INSTDIR" "$Inventario" "$PLUGINSDIR\contenido.zip" ${TAMANO_BYTES}'
  Pop $0
  Pop $1
  ${If} $0 != 0
    !insertmacro Fallo "No se pudo instalar la demo. $1"
  ${EndIf}
  SetOutPath "$INSTDIR"
  ClearErrors
  WriteUninstaller "$INSTDIR\Desinstalar.exe"
  CreateDirectory "$SMPROGRAMS\YG Soluciones"
  CreateShortCut "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk" "$INSTDIR\app\java\bin\javaw.exe" '-jar "$INSTDIR\app\panel.jar"' "$INSTDIR\app\montana.ico"
  WriteRegStr HKCU "${REGKEY}" "DisplayName" "La Montaña Demo (Windows nativa)"
  WriteRegStr HKCU "${REGKEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "${REGKEY}" "Publisher" "YG Soluciones"
  WriteRegStr HKCU "${REGKEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${REGKEY}" "DisplayIcon" "$INSTDIR\app\montana.ico"
  WriteRegStr HKCU "${REGKEY}" "UninstallString" '"$INSTDIR\Desinstalar.exe"'
  ${If} ${Errors}
    !insertmacro Fallo "Los componentes quedaron en la carpeta elegida, pero Windows no pudo completar el registro. Volvé a ejecutar el instalador."
  ${EndIf}
SectionEnd
Section "Crear acceso directo en el escritorio" Escritorio
  ${If} $EscritorioAnterior == 1
  ${OrIfNot} ${FileExists} "$DESKTOP\La Montaña Demo.lnk"
    ClearErrors
    CreateShortCut "$DESKTOP\La Montaña Demo.lnk" "$INSTDIR\app\java\bin\javaw.exe" '-jar "$INSTDIR\app\panel.jar"' "$INSTDIR\app\montana.ico"
    ${IfNot} ${Errors}
      WriteRegDWORD HKCU "${REGKEY}" "AccesoEscritorio" 1
    ${EndIf}
  ${Else}
    DetailPrint "Se conservó el acceso existente con el mismo nombre en el escritorio."
  ${EndIf}
SectionEnd
Section -Preferencia
  ${IfNot} ${SectionIsSelected} ${Escritorio}
    ${If} $EscritorioAnterior == 1
      Delete "$DESKTOP\La Montaña Demo.lnk"
    ${EndIf}
    WriteRegDWORD HKCU "${REGKEY}" "AccesoEscritorio" 0
  ${EndIf}
SectionEnd
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
 !insertmacro MUI_DESCRIPTION_TEXT ${Programa} "Incluye la aplicación, la base de datos, el antivirus y el buzón de prueba."
 !insertmacro MUI_DESCRIPTION_TEXT ${Escritorio} "Abrí la demo desde el escritorio. También estará disponible desde Inicio."
!insertmacro MUI_FUNCTION_DESCRIPTION_END
Function .onInit
  !insertmacro Exclusivo
  InitPluginsDir
  StrCpy $Inventario "-"
  ReadRegStr $0 HKCU "${REGKEY}" "UninstallString"
  ReadRegStr $Instalada HKCU "${REGKEY}" "InstallLocation"
  ReadRegStr $1 HKCU "${REGKEY}" "DisplayVersion"
  ${If} $Instalada == ""
    ${If} $0 == '"${RAIZ}\Desinstalar.exe"'
      ${If} $1 != "0.1.5"
      ${AndIf} $1 != "0.1.6"
        !insertmacro Fallo "Hay otra versión registrada. Revisá su ubicación antes de instalar."
      ${EndIf}
      StrCpy $Instalada "${RAIZ}"
      StrCpy $Inventario "$PLUGINSDIR\archivos-$1.sha256.gz"
    ${ElseIf} $0 != ""
      !insertmacro Fallo "Hay otra versión registrada. Revisá su ubicación antes de instalar."
    ${EndIf}
  ${Else}
    ${If} $0 != '"$Instalada\Desinstalar.exe"'
      !insertmacro Fallo "La ubicación no coincide con la instalación registrada para este usuario."
    ${EndIf}
    ${If} $1 == "${VERSION}"
      StrCpy $Inventario "$PLUGINSDIR\actual.gz"
    ${ElseIf} $1 == "0.1.7"
      StrCpy $Inventario "$PLUGINSDIR\archivos-0.1.7.sha256.gz"
    ${Else}
      !insertmacro Fallo "Esta instalación pertenece a otra versión. Usá el instalador que le corresponde."
    ${EndIf}
  ${EndIf}
  ${If} $Instalada != ""
    StrCpy $INSTDIR $Instalada
  ${EndIf}
  ReadRegDWORD $EscritorioAnterior HKCU "${REGKEY}" "AccesoEscritorio"
  ${If} $EscritorioAnterior == 0
    !insertmacro UnselectSection ${Escritorio}
  ${EndIf}
FunctionEnd
Function un.onInit
  SetShellVarContext current
  ReadRegStr $Instalada HKCU "${REGKEY}" "InstallLocation"
  ${If} $Instalada == ""
  ${OrIf} $Instalada != $INSTDIR
    !insertmacro Fallo "La carpeta no coincide con la instalación registrada para este usuario."
  ${EndIf}
  !insertmacro Exclusivo
FunctionEnd
Section "Uninstall"
  InitPluginsDir
  SetOutPath "$PLUGINSDIR"
  File archivos.mjs
  File /oname=actual.gz "${INVENTARIO}"
  ClearErrors
  CopyFiles /SILENT "$INSTDIR\app\node\node.exe" "$PLUGINSDIR\node.exe"
  ${If} ${Errors}
    !insertmacro Fallo "No se encontró el componente que retira la demo. Repará esta versión y volvé a intentar."
  ${EndIf}
  nsExec::ExecToStack '"$SYSDIR\certutil.exe" -hashfile "$PLUGINSDIR\node.exe" SHA256'
  Pop $0
  Pop $1
  ${UnStrStr} $2 $1 "${NODE_SHA256}"
  ${If} $0 != 0
  ${OrIf} $2 == ""
    !insertmacro Fallo "No se pudo verificar el componente de desinstalación. Repará esta versión y volvé a intentar."
  ${EndIf}
  !insertmacro Detener "$PLUGINSDIR\node.exe" "$PLUGINSDIR\actual.gz"
  nsExec::ExecToStack '"$PLUGINSDIR\node.exe" "$PLUGINSDIR\archivos.mjs" retirar "$INSTDIR" "$PLUGINSDIR\actual.gz"'
  Pop $0
  Pop $1
  ${If} $0 != 0
    !insertmacro Fallo "No se pudo completar la desinstalación. $1"
  ${EndIf}
  Delete "$SMPROGRAMS\YG Soluciones\La Montaña Demo.lnk"
  RMDir "$SMPROGRAMS\YG Soluciones"
  ReadRegDWORD $0 HKCU "${REGKEY}" "AccesoEscritorio"
  ${If} $0 == 1
    Delete "$DESKTOP\La Montaña Demo.lnk"
  ${EndIf}
  DeleteRegKey HKCU "${REGKEY}"
  Delete "$INSTDIR\Desinstalar.exe"
  RMDir "$INSTDIR"
SectionEnd
