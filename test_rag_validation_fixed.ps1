# Script de Tests et Validation RAG Avancés
$tests = @(
    # Tests de cohérence sémantique (RAG)
    @{question="Explique le champ Identifiant unique du msgType 53"; expectedMsgType="53"; expectedChamp="Identifiant unique"; testType="champ-precis"},
    @{question="Explique le champ Montant de l'opération du msgType 16"; expectedMsgType="16"; expectedChamp="Montant de l'opération"; testType="champ-precis"},
    @{question="Que signifie le Champ 4 du msgType 03"; expectedMsgType="03"; expectedChamp="Champ 4"; testType="champ-precis"},
    @{question="Explique le Type de message du msgType A3"; expectedMsgType="A3"; expectedChamp="Type de message"; testType="champ-precis"},
    
    # Tests de validation stricte du msgType
    @{question="Information sur le msgType 53 uniquement"; expectedMsgType="53"; expectedChamp=$null; testType="msgtype-explicite"},
    @{question="Explique msgType 16 sans parler des autres"; expectedMsgType="16"; expectedChamp=$null; testType="msgtype-explicite"},
    
    # Tests de questions ambiguës (doivent être refusées)
    @{question="Information sur les identifiants en général"; expectedMsgType=$null; expectedChamp=$null; testType="ambigue"; shouldRefuse=$true},
    @{question="Détails sur tous les msgTypes"; expectedMsgType=$null; expectedChamp=$null; testType="ambigue"; shouldRefuse=$true},
    @{question="Donne-moi un résumé global"; expectedMsgType=$null; expectedChamp=$null; testType="ambigue"; shouldRefuse=$true},
    
    # Tests de questions comparatives (doivent être refusées)
    @{question="Différence entre msgType 53 et 16"; expectedMsgType=$null; expectedChamp=$null; testType="comparative"; shouldRefuse=$true},
    @{question="Comparaison msgType A3 versus 03"; expectedMsgType=$null; expectedChamp=$null; testType="comparative"; shouldRefuse=$true},
    
    # Tests de champs inexistants
    @{question="Explique le champ Inexistant du msgType 53"; expectedMsgType="53"; expectedChamp="Champ Inexistant"; testType="champ-inexistant"; shouldRefuse=$true},
    @{question="Quel est le rôle du Champ 99 du msgType 16"; expectedMsgType="16"; expectedChamp="Champ 99"; testType="champ-inexistant"; shouldRefuse=$true}
)

Write-Host "🚀 DÉMARRAGE DES TESTS DE VALIDATION RAG" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray

$stats = @{
    total = 0
    success = 0
    refused = 0
    errors = 0
    perfWarnings = 0
    embeddingTimes = @()
    searchTimes = @()
    llmTimes = @()
    totalTimes = @()
}

foreach ($test in $tests) {
    $stats.total++
    Write-Host "`n📋 Test $($stats.total) : $($test.testType.ToUpper())" -ForegroundColor Yellow
    Write-Host "Question: $($test.question)" -ForegroundColor White
    
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
        
        # Analyse de la réponse
        $success = $false
        $refused = $false
        $correctMsgType = $false
        $correctChamp = $false
        $noHallucination = $false
        
        if ($response.success) {
            $success = $true
            
            # Vérification du refus attendu
            if ($test.shouldRefuse) {
                if ($response.answer -like "*Désolé*") {
                    $refused = $true
                    $stats.success++
                    Write-Host "✅ Refus correct (question ambiguë)" -ForegroundColor Green
                } else {
                    Write-Host "❌ Aurait dû refuser cette question ambiguë" -ForegroundColor Red
                }
            } else {
                # Vérification du msgType
                if ($response.contexts -and $response.contexts.Count -gt 0) {
                    $topContext = $response.contexts[0]
                    if ($topContext.msgType -eq $test.expectedMsgType) {
                        $correctMsgType = $true
                    }
                    
                    # Vérification du champ (si applicable)
                    if ($test.expectedChamp -and $response.answer -like "*$($test.expectedChamp)*") {
                        $correctChamp = $true
                    }
                    
                    # Vérification de l'absence d'hallucination
                    if ($response.answer -notlike "*Désolé*" -and $response.answer.Length -gt 10) {
                        $noHallucination = $true
                    }
                }
                
                if ($correctMsgType -and ($test.expectedChamp -eq $null -or $correctChamp)) {
                    $stats.success++
                    Write-Host "✅ Réponse correcte" -ForegroundColor Green
                } else {
                    Write-Host "❌ Réponse incorrecte" -ForegroundColor Red
                }
            }
            
            # Collecte des métriques
            if ($response.metadata) {
                $stats.embeddingTimes += $response.metadata.embeddingTimeMs
                $stats.searchTimes += $response.metadata.searchTimeMs
                $stats.llmTimes += $response.metadata.llmTimeMs
                $stats.totalTimes += $response.metadata.totalTimeMs
                
                # Validation des seuils de performance
                if ($response.metadata.embeddingTimeMs -gt 1500) { $stats.perfWarnings++ }
                if ($response.metadata.searchTimeMs -gt 100) { $stats.perfWarnings++ }
                if ($response.metadata.llmTimeMs -gt 50) { $stats.perfWarnings++ }
                if ($response.metadata.totalTimeMs -gt 2000) { $stats.perfWarnings++ }
            }
            
            Write-Host "Réponse: $($response.answer)" -ForegroundColor Cyan
            if ($response.contexts -and $response.contexts.Count -gt 0) {
                Write-Host "Top msgType: $($response.contexts[0].msgType) (score: $([math]::Round($response.contexts[0].score, 3)))" -ForegroundColor Blue
            }
            
        } else {
            $stats.errors++
            Write-Host "❌ Erreur API: $($response.error)" -ForegroundColor Red
        }
        
    } catch {
        $stats.errors++
        Write-Host "❌ Exception: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host "Verdict: MsgType=$correctMsgType, Champ=$correctChamp, Refus=$refused, NoHallucination=$noHallucination" -ForegroundColor Gray
    Write-Host "-" * 60 -ForegroundColor Gray
}

# Résumé final
Write-Host "`n📊 RÉSULTATS DE VALIDATION" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray

Write-Host "Tests totaux: $($stats.total)" -ForegroundColor White
Write-Host "Succès: $($stats.success) ($([math]::Round(($stats.success/$stats.total)*100, 1))%)" -ForegroundColor $(if($stats.success -gt ($stats.total/2)) {"Green"} else {"Red"})
Write-Host "Refus corrects: $($stats.refused)" -ForegroundColor Yellow
Write-Host "Erreurs: $($stats.errors)" -ForegroundColor Red

if ($stats.embeddingTimes.Count -gt 0) {
    Write-Host "`n⚡ MÉTRIQUES DE PERFORMANCE" -ForegroundColor Cyan
    Write-Host "Embedding moyen: $([math]::Round(($stats.embeddingTimes | Measure-Object -Average).Average, 0))ms" -ForegroundColor White
    Write-Host "Recherche moyenne: $([math]::Round(($stats.searchTimes | Measure-Object -Average).Average, 0))ms" -ForegroundColor White
    Write-Host "LLM moyen: $([math]::Round(($stats.llmTimes | Measure-Object -Average).Average, 0))ms" -ForegroundColor White
    Write-Host "Total moyen: $([math]::Round(($stats.totalTimes | Measure-Object -Average).Average, 0))ms" -ForegroundColor White
    Write-Host "Alertes performance: $($stats.perfWarnings)" -ForegroundColor $(if($stats.perfWarnings -eq 0) {"Green"} else {"Yellow"})
}

Write-Host "`n🎯 VALIDATION TERMINÉE" -ForegroundColor Cyan
