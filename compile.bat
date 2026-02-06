@echo off
echo Compilation du projet...
echo.

echo Nettoyage et compilation avec Maven...
call mvn clean compile

echo.
echo Compilation terminee.
pause
