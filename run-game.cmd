@echo off
echo Setting up Java 23 environment...
set JAVA_HOME=C:\Users\Tuf\.jdks\corretto-23.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME = %JAVA_HOME%
echo.
echo Starting Pacman Game...
call mvnw.cmd clean javafx:run
pause
