@echo off
curl.exe -X POST "http://localhost:8080/api/gemini-transform/transform-simple" -H "Content-Type: application/json" -d "{\"geminiJson\":\"{\\\"fields\\\":[\\\"Type de message\\\",\\\"Code de traitement\\\",\\\"Référence de transaction\\\",\\\"Montant\\\",\\\"Date de la transaction\\\",\\\"Identifiant du commerçant\\\"],\\\"values\\\":[\\\"16\\\",\\\"002\\\",\\\"ABC123\\\",\\\"1500\\\",\\\"2026-02-06\\\",\\\"M12345\\\"]}\"}"
pause
