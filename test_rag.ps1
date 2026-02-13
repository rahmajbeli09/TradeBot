# Script de test RAG pour PowerShell
$tests = @(
    @{question="Explique le champ Identifiant unique du msgType 53"; expected="53"},
    @{question="Explique le champ Montant de l'opération du msgType 16"; expected="16"},
    @{question="Que signifie le Champ 4 du msgType 03"; expected="03"},
    @{question="Explique le Type de message du msgType A3"; expected="A3"},
    @{question="Quelle est la différence entre msgType 53 et 16"; expected="53"},
    @{question="Information sur le statut de l'opération"; expected="16"},
    @{question="Explique le champ Quantité du mouvement"; expected="03"},
    @{question="Quel est le rôle du Champ 1"; expected="53"},
    @{question="Information sur les identifiants"; expected="53"}
)

foreach ($test in $tests) {
    Write-Host "`n=== Test: $($test.question) ===" -ForegroundColor Cyan
    
    $body = @{
        question = $test.question
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
        
        Write-Host "Question: $($response.question)" -ForegroundColor Yellow
        Write-Host "Réponse: $($response.answer)" -ForegroundColor Green
        Write-Host "Success: $($response.success)" -ForegroundColor $(if($response.success) {"Green"} else {"Red"})
        
        if ($response.contexts -and $response.contexts.Count -gt 0) {
            $topContext = $response.contexts[0]
            Write-Host "Top msgType: $($topContext.msgType) (score: $([math]::Round($topContext.score, 3)))" -ForegroundColor Blue
            Write-Host "Expected: $($test.expected)" -ForegroundColor Magenta
            Write-Host "Correct: $(if($topContext.msgType -eq $test.expected) {"✅"} else {"❌"})" -ForegroundColor $(if($topContext.msgType -eq $test.expected) {"Green"} else {"Red"})
        }
        
        if ($response.metadata) {
            Write-Host "Temps: embedding=$($response.metadata.embeddingTimeMs)ms, search=$($response.metadata.searchTimeMs)ms, llm=$($response.metadata.llmTimeMs)ms, total=$($response.metadata.totalTimeMs)ms" -ForegroundColor Gray
        }
        
    } catch {
        Write-Host "ERREUR: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host "----------------------------------------"
}
