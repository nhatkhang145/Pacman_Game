@echo off
echo Setting up Java 23 environment...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-23.0.2.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME = %JAVA_HOME%
echo.
echo Starting Pacman Game...
call mvnw.cmd clean javafx:run
pause
