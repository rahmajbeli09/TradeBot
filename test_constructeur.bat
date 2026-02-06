@echo off
echo Test du constructeur ReadableFeedLine...
echo.

echo Compilation du projet...
call mvn clean compile -q

if %ERRORLEVEL% EQU 0 (
    echo ✅ Compilation reussie ! Le constructeur est correct.
) else (
    echo ❌ Erreur de compilation persiste
    echo Verifiez les logs ci-dessus pour details
)

echo.
pause
