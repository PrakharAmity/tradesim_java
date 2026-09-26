@echo off
setlocal
cd /d "%~dp0\.."
java -XX:+UseSerialGC -Xms16m -Xmx64m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -cp target\tradesim.jar com.tradesim.TestRunner
