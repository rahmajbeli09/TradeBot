# Script de test simple RAG
$tests = @(
    @{question="Explique le champ Identifiant unique du msgType 53"; expectedMsgType="53"},
    @{question="Explique le champ Montant de l'opération du msgType 16"; expectedMsgType="16"},
    @{question="Information sur les identifiants en général"; shouldRefuse=$true},
    @{question="Différence entre msgType 53 et 16"; shouldRefuse=$true}
)

Write-Host "🚀 TESTS RAG VALIDATION" -ForegroundColor Cyan

foreach ($test in $tests) {
    Write-Host "`nQuestion: $($test.question)" -ForegroundColor Yellow
    
    try {
        $body = @{
            question = $test.question
            limit = 3
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/rag/ask" `
            -Method POST `
            -Headers @{
                "Content-Type" = "application/json"
                "Authorization" = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
            } `
            -Body $body `
            -TimeoutSec 30
        
        if ($response.success) {
            Write-Host "Réponse: $($response.answer)" -ForegroundColor Green
            if ($response.contexts -and $response.contexts.Count -gt 0) {
                Write-Host "Top msgType: $($response.contexts[0].msgType) (score: $([math]::Round($response.contexts[0].score, 3)))" -ForegroundColor Blue
            }
            
            # Vérifications
            if ($test.shouldRefuse) {
                if ($response.answer -like "*Désolé*") {
                    Write-Host "✅ Refus correct" -ForegroundColor Green
                } else {
                    Write-Host "❌ Aurait dû refuser" -ForegroundColor Red
                }
            } elseif ($test.expectedMsgType) {
                if ($response.contexts[0].msgType -eq $test.expectedMsgType) {
                    Write-Host "✅ MsgType correct" -ForegroundColor Green
                } else {
                    Write-Host "❌ MsgType incorrect" -ForegroundColor Red
                }
            }
        } else {
            Write-Host "❌ Erreur: $($response.error)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Exception: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n🎯 TESTS TERMINÉS" -ForegroundColor Cyan
