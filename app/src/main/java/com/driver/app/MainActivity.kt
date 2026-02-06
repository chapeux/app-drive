package com.driver.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

// Deixe a Data Class aqui se não for criar um arquivo separado
data class Offer(val valor: Double, val distancia: Double, val classification: String, val timestamp: Long) {
    fun toJson(): JSONObject = JSONObject().apply { 
        put("valor", valor); put("distancia", distancia); put("classification", classification); put("timestamp", timestamp) 
    }
    companion object { 
        fun fromJson(o: JSONObject): Offer = Offer(o.getDouble("valor"), o.getDouble("distancia"), o.getString("classification"), o.getLong("timestamp")) 
    }
}

class MainActivity : AppCompatActivity() {
    private val PREFS = "driver_prefs"
    private val KEY_HISTORY = "offers_history"
    private lateinit var adapter: ArrayAdapter<String>
    private val offersList = mutableListOf<Offer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botão 1: Ativar Acessibilidade (Leitura de tela)
        val btnEnable = findViewById<Button>(R.id.buttonEnableService)
        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Botão 2: Ativar Overlay (Janela flutuante)
        val btnOverlay = findViewById<Button>(R.id.buttonOverlayPermission) // Crie este ID no seu XML
        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
            }
        }

        val listView = findViewById<ListView>(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        updateUI()
    }

    // Atualiza a lista sempre que o motorista voltar para o app
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        offersList.clear()
        offersList.addAll(loadHistory())
        adapter.clear()
        adapter.addAll(offersList.map { formatOffer(it) })
        adapter.notifyDataSetChanged()
    }

    private fun formatOffer(o: Offer): String = "R$ %.2f — %.1f km — %s".format(o.valor, o.distancia, o.classification)

    private fun loadHistory(): List<Offer> {
        val s = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            val out = mutableListOf<Offer>()
            for (i in 0 until arr.length()) out.add(Offer.fromJson(arr.getJSONObject(i)))
            out
        } catch (e: Exception) {
            emptyList()
        }
    }
}