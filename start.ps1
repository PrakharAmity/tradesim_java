$PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $PSScriptRoot

if (!(Test-Path "target\tradesim.jar")) {
    Write-Host "[TradeSim] Compiling and packaging TradeSim..." -ForegroundColor Cyan
    if (!(Test-Path "target\classes")) { New-Item -ItemType Directory -Path "target\classes" -Force | Out-Null }
    $files = (Get-ChildItem -Recurse -Filter "*.java" src\main\java).FullName
    javac -d target\classes $files
    Copy-Item -Recurse -Force src\main\resources\* target\classes\
    
    $jarPath = "jar"
    if (!(Get-Command jar -ErrorAction SilentlyContinue)) {
        if (Test-Path "C:\Program Files\Java\jdk-18.0.2.1\bin\jar.exe") {
            $jarPath = "C:\Program Files\Java\jdk-18.0.2.1\bin\jar.exe"
        }
    }
    & $jarPath --create --file target\tradesim.jar --main-class com.tradesim.Main -C target\classes .
}

Write-Host "[TradeSim] Starting TradeSim server on http://localhost:3000 ..." -ForegroundColor Green
java -jar target\tradesim.jar
