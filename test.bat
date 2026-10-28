@echo off
cd /d "%~dp0"
if not exist out\test mkdir out\test
(for /r src %%f in (*.java) do @echo "%%f") > out\test-sources.txt
javac --release 21 -encoding UTF-8 -d out\test @out\test-sources.txt
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp out\test com.lemanty.aipet.TaskManagerTest
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp out\test com.lemanty.aipet.WindowFrameBufferTest
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp out\test com.lemanty.aipet.ConversationPickerTest
