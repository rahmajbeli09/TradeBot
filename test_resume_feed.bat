@echo off
echo Test du service ResumeFeed...
echo.

echo 1. Verification du statut du service:
curl.exe -X GET "http://localhost:8080/api/resume-feed/status"
echo.

echo.
echo 2. Liste des fichiers FEED disponibles:
curl.exe -X GET "http://localhost:8080/api/resume-feed/list-files"
echo.

echo.
echo 3. Test de resume pour le fichier FEED_TEST_04.txt:
curl.exe -X GET "http://localhost:8080/api/resume-feed/generate/FEED_TEST_04.txt"
echo.

pause
