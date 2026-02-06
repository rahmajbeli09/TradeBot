@echo off
echo Test de compilation finale...
echo.

echo Nettoyage et compilation...
call mvn clean compile -q

if %ERRORLEVEL% EQU 0 (
    echo ✅ Compilation reussie ! Le probleme est resolu.
) else (
    echo ❌ Erreur de compilation persiste
    echo Verifiez les logs ci-dessus pour details
)

echo.
pause
