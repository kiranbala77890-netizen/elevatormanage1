@echo off
REM Compile and run without Maven. Put sqlite-jdbc and slf4j-api jars in lib\ first (see README).
REM Usage: run.bat            -> GUI
REM        run.bat --console 30
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
java -cp "out;lib/*" elevator.sim.Main %*
