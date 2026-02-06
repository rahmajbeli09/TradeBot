@echo off
echo Test du service FeedReadable...
echo.

echo 1. Verification du statut du service:
curl.exe -X GET "http://localhost:8080/api/feed-readable/status"
echo.

echo.
echo 2. Liste des fichiers FEED disponibles:
curl.exe -X GET "http://localhost:8080/api/feed-readable/list-files"
echo.

echo.
echo 3. Test de lecture lisible du fichier FEED_TEST_04.txt:
curl.exe -X GET "http://localhost:8080/api/feed-readable/generate/FEED_TEST_04.txt"
echo.

pause
