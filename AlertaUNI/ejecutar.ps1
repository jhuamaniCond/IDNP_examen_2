# Compila AlertaUNI, abre un emulador Android (si no hay uno corriendo),
# instala la app y la lanza.
#
# Uso:  .\ejecutar.ps1                 (usa el primer AVD disponible)
#       .\ejecutar.ps1 -Avd Medium_Phone
#       .\ejecutar.ps1 -Limpio         (hace 'clean' antes de compilar)

param(
    [string]$Avd = "",
    [switch]$Limpio
)

$ErrorActionPreference = "Stop"
$Proyecto = $PSScriptRoot
$Paquete  = "com.erns.alertauni"
$Actividad = "$Paquete/.MainActivity"

function Paso($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

# --- SDK de Android -------------------------------------------------------
$Sdk = $env:ANDROID_HOME
if (-not $Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
if (-not $Sdk) { $Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not (Test-Path $Sdk)) { throw "No se encontro el SDK de Android. Define ANDROID_HOME." }
$env:ANDROID_HOME = $Sdk

$Adb      = Join-Path $Sdk "platform-tools\adb.exe"
$Emulador = Join-Path $Sdk "emulator\emulator.exe"
if (-not (Test-Path $Adb))      { throw "No se encontro adb en $Adb" }
if (-not (Test-Path $Emulador)) { throw "No se encontro el emulador en $Emulador" }

# Gradle necesita local.properties con sdk.dir
$LocalProps = Join-Path $Proyecto "local.properties"
if (-not (Test-Path $LocalProps)) {
    Paso "Creando local.properties"
    "sdk.dir=" + ($Sdk -replace '\\', '\\') | Out-File $LocalProps -Encoding ascii
}

# --- Java: kapt con Kotlin 2.0 no soporta JDK 25+, usar 17 o 21 ----------------
$Candidatos = @(
    (Join-Path $env:USERPROFILE ".jdks"),
    "C:\Program Files\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Microsoft",
    "C:\Program Files\Zulu"
) | Where-Object { Test-Path $_ } |
    ForEach-Object { Get-ChildItem $_ -Directory } |
    Where-Object { $_.Name -match "(^|[^0-9])(17|21)(\.|$|[^0-9])" -and (Test-Path (Join-Path $_.FullName "bin\java.exe")) } |
    Sort-Object Name -Descending
$Jdk = $Candidatos | Select-Object -First 1
if ($Jdk) {
    $env:JAVA_HOME = $Jdk.FullName
} elseif (-not $env:JAVA_HOME) {
    Write-Warning "No se encontro JDK 17/21; se usara el JBR de Android Studio (puede fallar kapt)."
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
}
Paso "Usando Java en $env:JAVA_HOME"

# --- Emulador ---------------------------------------------------------------
function Get-Emuladores {
    & $Adb devices | Select-String "^emulator-\d+\s+device" | ForEach-Object { ($_ -split "\s+")[0] }
}

& $Adb start-server | Out-Null
$Serial = Get-Emuladores | Select-Object -First 1

if (-not $Serial) {
    if (-not $Avd) {
        $Avd = & $Emulador -list-avds | Where-Object { $_ -and $_ -notmatch "^INFO" } | Select-Object -First 1
        if (-not $Avd) { throw "No hay AVDs creados. Crea uno en Android Studio > Device Manager." }
    }
    Paso "Iniciando emulador '$Avd'"
    Start-Process -FilePath $Emulador -ArgumentList "-avd", $Avd -WindowStyle Minimized

    Paso "Esperando a que el emulador arranque"
    $limite = (Get-Date).AddMinutes(5)
    while (-not ($Serial = Get-Emuladores | Select-Object -First 1)) {
        if ((Get-Date) -gt $limite) { throw "El emulador no aparecio en 5 minutos." }
        Start-Sleep -Seconds 2
    }
    while ((& $Adb -s $Serial shell getprop sys.boot_completed 2>$null) -ne "1") {
        if ((Get-Date) -gt $limite) { throw "El emulador no termino de arrancar en 5 minutos." }
        Start-Sleep -Seconds 2
    }
} else {
    Paso "Usando emulador ya abierto: $Serial"
}
$env:ANDROID_SERIAL = $Serial

# --- Compilar e instalar ----------------------------------------------------
Push-Location $Proyecto
try {
    $tareas = @()
    if ($Limpio) { $tareas += "clean" }
    $tareas += ":app:installDebug"
    Paso "Compilando e instalando ($($tareas -join ' '))"
    # La ruta del proyecto tiene tildes; el plugin de Android lo bloquea por defecto
    & .\gradlew.bat @tareas "-Pandroid.overridePathCheck=true"
    if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion/instalacion (codigo $LASTEXITCODE)." }
} finally {
    Pop-Location
}

# --- Lanzar la app ------------------------------------------------------------
# adb escribe avisos en stderr; con "Stop" PowerShell 5.1 los trataria como error
function AdbShell {
    $previo = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { & $Adb -s $Serial shell @args 2>&1 | ForEach-Object { "$_" } }
    finally { $ErrorActionPreference = $previo }
}

function Test-AppEnPantalla {
    $foco = AdbShell dumpsys window | Select-String "mCurrentFocus" | Out-String
    $pid_ = AdbShell pidof $Paquete
    return ($foco -match [regex]::Escape($Paquete)) -and $pid_
}

if (-not (AdbShell pm path $Paquete | Select-String "^package:")) {
    throw "AlertaUNI no quedo instalada en $Serial."
}

Paso "Cerrando las demas apps del emulador"
AdbShell input keyevent KEYCODE_HOME | Out-Null
AdbShell pm list packages -3 |
    Where-Object { $_ -match "^package:(.+)$" } |
    ForEach-Object { $Matches[1].Trim() } |
    Where-Object { $_ -ne $Paquete } |
    ForEach-Object { AdbShell am force-stop $_ | Out-Null }

Paso "Abriendo AlertaUNI"
# -S reinicia la app si ya estaba abierta; -W espera a que la actividad cargue
AdbShell am start -W -S -n $Actividad | Out-Null

Paso "Comprobando que AlertaUNI este en pantalla"
$limite = (Get-Date).AddSeconds(40)
$reintentado = $false
while (-not (Test-AppEnPantalla)) {
    if ((Get-Date) -gt $limite) {
        Write-Host "Ventana enfocada:" -ForegroundColor Yellow
        AdbShell dumpsys window | Select-String "mCurrentFocus"
        Write-Host "Ultimos errores de la app (logcat crash):" -ForegroundColor Yellow
        AdbShell logcat -d -b crash | Select-Object -Last 30
        throw "AlertaUNI no aparecio en pantalla. Si el emulador muestra 'isn't responding', pulsa Wait y vuelve a ejecutar."
    }
    # Un dialogo del sistema (ANR) puede tapar la app: se cierra y se reintenta una vez
    if (-not $reintentado -and (Get-Date) -gt $limite.AddSeconds(-25)) {
        AdbShell input keyevent KEYCODE_BACK | Out-Null
        AdbShell am start -W -n $Actividad | Out-Null
        $reintentado = $true
    }
    Start-Sleep -Seconds 2
}
Write-Host "Listo. AlertaUNI esta abierta y en primer plano en $Serial." -ForegroundColor Green
