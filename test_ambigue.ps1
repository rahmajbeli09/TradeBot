# Test simple pour question ambiguë
$body = @{
    question = "Information sur les identifiants"
    limit = 3
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/rag/ask" `
        -Method POST `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
        } `
        -Body $body `
        -TimeoutSec 30
    
    Write-Host "Réponse: $($response.answer)" -ForegroundColor Green
    if ($response.success) {
        Write-Host "✅ Succès" -ForegroundColor Green
    } else {
        Write-Host "❌ Erreur: $($response.error)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Exception: $($_.Exception.Message)" -ForegroundColor Red
}
