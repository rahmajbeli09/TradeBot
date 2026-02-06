@echo off
echo Test du service ResumeFeed Ameliore...
echo.

echo 1. Verification du statut du service ameliore:
curl.exe -X GET "http://localhost:8080/api/resume-feed-ameliore/status"
echo.

echo.
echo 2. Liste des fichiers FEED disponibles:
curl.exe -X GET "http://localhost:8080/api/resume-feed-ameliore/list-files"
echo.

echo.
echo 3. Test de resume ameliore pour le fichier FEED_TEST_04.txt:
curl.exe -X GET "http://localhost:8080/api/resume-feed-ameliore/generate/FEED_TEST_04.txt"
echo.

pause
