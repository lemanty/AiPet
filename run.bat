@echo off
cd /d "%~dp0"
if not exist out\main mkdir out\main
(for /r src\main\java %%f in (*.java) do @echo "%%f") > out\sources.txt
javac --release 21 -encoding UTF-8 -d out\main @out\sources.txt
if errorlevel 1 exit /b 1
java -cp out\main com.lemanty.aipet.App
