@echo off
setlocal
cd /d "%~dp0"

if not exist "target\tradesim.jar" (
    echo [TradeSim] Compiling and packaging TradeSim...
    if not exist "target\classes" mkdir target\classes
    javac -d target\classes src\main\java\com\tradesim\*.java src\main\java\com\tradesim\data\*.java src\main\java\com\tradesim\http\*.java src\main\java\com\tradesim\model\*.java src\main\java\com\tradesim\policy\*.java src\main\java\com\tradesim\strategy\*.java
    xcopy /E /I /Y src\main\resources target\classes >nul
    "C:\Program Files\Java\jdk-18.0.2.1\bin\jar.exe" --create --file target\tradesim.jar --main-class com.tradesim.Main -C target\classes .
)

echo [TradeSim] Starting TradeSim server on http://localhost:3000 ...
java -XX:+UseSerialGC -Xms16m -Xmx64m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss256k -jar target\tradesim.jar
