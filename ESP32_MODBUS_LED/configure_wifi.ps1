# Configure the ESP32 deck-light WiFi credentials over the serial port from
# Windows. Pure PowerShell, no installs needed.
#
# Two ways to run:
#
#   1. Fully interactive (just double-click in Explorer or run with no args):
#         .\configure_wifi.ps1
#      The script lists detected serial ports, then prompts for the COM port,
#      SSID, and password (password input is masked).
#
#   2. Scripted / non-interactive:
#         .\configure_wifi.ps1 -Port COM3 -Ssid "MyNetwork" -Password "secret"
#
# Optional flags:
#   -BaudRate 115200      default matches the sketch
#   -SkipConnect          just save creds, don't trigger 'connect'
#
# What it does, in order:
#   1. Opens <Port> at 115200 8N1.
#   2. Sends   ssid <Ssid>
#   3. Sends   pass <Password>
#   4. Sends   connect       (unless -SkipConnect)
#   5. Reads back ~3s of output and prints it so you can see the IP.
#
# Tip: while the script holds the port open, the Arduino IDE Serial Monitor
# cannot be open at the same time on the same COM port. Close one before the
# other.

param(
    [string]$Port,
    [string]$Ssid,
    [string]$Password,
    [int]$BaudRate = 115200,
    [switch]$SkipConnect
)

$ErrorActionPreference = "Stop"

function Get-AvailablePorts {
    try {
        return [System.IO.Ports.SerialPort]::GetPortNames() | Sort-Object
    } catch {
        return @()
    }
}

function Read-PlainPassword {
    param([string]$Prompt)
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $bstr   = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

# ---------- Interactive prompts for anything not passed in ----------
if (-not $Port) {
    $ports = Get-AvailablePorts
    if ($ports.Count -gt 0) {
        Write-Host "Detected serial ports:"
        for ($i = 0; $i -lt $ports.Count; $i++) {
            Write-Host ("  [{0}] {1}" -f ($i + 1), $ports[$i])
        }
        $choice = Read-Host "Pick a port number, or type a port name (e.g. COM3)"
        if ($choice -match '^\d+$') {
            $idx = [int]$choice - 1
            if ($idx -ge 0 -and $idx -lt $ports.Count) {
                $Port = $ports[$idx]
            }
        }
        if (-not $Port) { $Port = $choice }
    } else {
        Write-Host "No serial ports detected. Plug in the ESP32 and try again, or enter the port name manually."
        $Port = Read-Host "Port name (e.g. COM3)"
    }
}

if (-not $Ssid) {
    $Ssid = Read-Host "WiFi SSID"
}

if (-not $Password) {
    $Password = Read-PlainPassword "WiFi password"
}

if (-not $Port -or -not $Ssid) {
    throw "Port and SSID are required."
}

Write-Host ""
Write-Host ("Configuring {0} on {1} ..." -f $Ssid, $Port)

# ---------- Talk to the device ----------
$serial = New-Object System.IO.Ports.SerialPort $Port, $BaudRate, "None", 8, "One"
$serial.NewLine = "`n"
$serial.ReadTimeout = 500
$serial.DtrEnable = $true   # avoid auto-reset deassert on some boards
$serial.RtsEnable = $true

try {
    $serial.Open()
    Start-Sleep -Milliseconds 1500    # let any boot banner flush

    $serial.WriteLine("ssid $Ssid")
    Start-Sleep -Milliseconds 200

    $serial.WriteLine("pass $Password")
    Start-Sleep -Milliseconds 200

    if (-not $SkipConnect) {
        $serial.WriteLine("connect")
        Start-Sleep -Seconds 3
    } else {
        Start-Sleep -Milliseconds 500
    }

    try {
        $output = $serial.ReadExisting()
        if ($output) { Write-Host $output }
    } catch {}
}
finally {
    if ($serial.IsOpen) { $serial.Close() }
    $serial.Dispose()
}
