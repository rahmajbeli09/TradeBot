@echo off
echo Test de compilation du DTO ReadableFeedLine...
echo.

echo Compilation du projet...
call mvn clean compile -q

if %ERRORLEVEL% EQU 0 (
    echo ✅ Compilation reussie !
) else (
    echo ❌ Erreur de compilation
)

echo.
pause
