@echo off
setlocal
cd /d "%~dp0\.."
java -cp target\tradesim.jar com.tradesim.TestRunner
