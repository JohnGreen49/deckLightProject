# Configure the ESP32 deck-light WiFi credentials over the serial port from
# Windows. Pure PowerShell, no installs needed.
#
# Usage (run from a Windows PowerShell prompt; PowerShell 5.1 or newer):
#
#   .\configure_wifi.ps1 -Port COM3 -Ssid "MyNetwork" -Password "mySecret"
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
# other. To find the port: list with   Get-PnpDevice -Class Ports

param(
    [Parameter(Mandatory=$true)][string]$Port,
    [Parameter(Mandatory=$true)][string]$Ssid,
    [Parameter(Mandatory=$true)][string]$Password,
    [int]$BaudRate = 115200,
    [switch]$SkipConnect
)

$ErrorActionPreference = "Stop"

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

    # Drain whatever the device sent us back so the user sees the result.
    try {
        $output = $serial.ReadExisting()
        if ($output) { Write-Host $output }
    } catch {}
}
finally {
    if ($serial.IsOpen) { $serial.Close() }
    $serial.Dispose()
}
