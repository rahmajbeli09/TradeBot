@echo off
echo Test du service AnonymisationFeed...
echo.

echo 1. Verification du statut du service d'anonymisation:
curl.exe -X GET "http://localhost:8080/api/anonymiser-feed/status"
echo.

echo.
echo 2. Liste des fichiers FEED disponibles:
curl.exe -X GET "http://localhost:8080/api/anonymiser-feed/list-files"
echo.

echo.
echo 3. Test d'anonymisation pour le fichier FEED_TEST_04.txt:
curl.exe -X GET "http://localhost:8080/api/anonymiser-feed/anonymiser/FEED_TEST_04.txt"
echo.

echo.
echo 4. Affichage du contenu anonymise (premier extrait):
curl.exe -X GET "http://localhost:8080/api/anonymiser-feed/anonymiser/FEED_TEST_04.txt" | jq -r ".contenuAnonymise" | head -5

pause
