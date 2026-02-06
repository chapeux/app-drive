package com.driver.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import org.json.JSONArray

class UberAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Filtramos para processar apenas quando a janela mudar ou o conteúdo for atualizado
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            findUberData(rootNode)
        }
    }

    private fun findUberData(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // Verifica se o elemento atual possui texto
        node.text?.let {
            extractDataAndCalculate(it.toString())
        }

        // Percorre todos os elementos filhos da árvore de visualização
        for (i in 0 until node.childCount) {
            findUberData(node.getChild(i))
        }
    }

    private fun extractDataAndCalculate(text: String) {
        // Regex para capturar valores monetários (Ex: R$ 15,50 ou 15.50)
        val priceRegex = Regex("""(?:R\$\s?)?(\d+[.,]\d{2})""")
        // Regex para capturar distâncias (Ex: 4,5 km ou 4.5km)
        val distanceRegex = Regex("""(\d+[.,]\d+)\s?km""")

        val priceMatch = priceRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
        val distanceMatch = distanceRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()

        if (priceMatch != null && distanceMatch != null) {
            processNewOffer(priceMatch, distanceMatch)
        }
    }

    private fun processNewOffer(valor: Double, distancia: Double) {
        // Cálculo de rentabilidade (R$ por KM)
        val reaisPorKm = valor / distancia
        
        val classification = when {
            reaisPorKm < 1.5 -> "VERMELHO" // Ruim
            reaisPorKm > 2.2 -> "VERDE"    // Excelente
            else -> "AMARELO"              // Médio
        }

        // 1. Salva no histórico (SharedPreferences) para a MainActivity
        saveToHistory(valor, distancia, classification)

        // 2. Envia para o OverlayService para mostrar na tela da Uber
        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            putExtra("valor", valor)
            putExtra("classification", classification)
        }
        startService(overlayIntent)

        Log.d("DriverApp", "Nova oferta capturada: R$$valor em $distancia km -> $classification")
    }

    private fun saveToHistory(valor: Double, distancia: Double, classification: String) {
        val prefs = getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
        val historyS = prefs.getString("offers_history", null)
        val historyArr = if (historyS != null) JSONArray(historyS) else JSONArray()

        // Evita duplicados: não salva se for idêntica à última oferta capturada
        if (historyArr.length() > 0) {
            val last = historyArr.getJSONObject(0)
            if (last.getDouble("valor") == valor && last.getDouble("distancia") == distancia) return
        }

        // Cria o novo objeto de oferta
        val newOffer = Offer(valor, distancia, classification, System.currentTimeMillis())
        
        // Reconstrói o JSON colocando a nova oferta no topo (posição 0)
        val newHistory = JSONArray()
        newHistory.put(newOffer.toJson())
        
        // Mantém as últimas 50 ofertas para não sobrecarregar a memória
        for (i in 0 until historyArr.length()) {
            if (i < 50) newHistory.put(historyArr.get(i))
        }

        prefs.edit().putString("offers_history", newHistory.toString()).apply()
    }

    override fun onInterrupt() {
        // Chamado quando o sistema interrompe o serviço (ex: economia de bateria)
    }
}