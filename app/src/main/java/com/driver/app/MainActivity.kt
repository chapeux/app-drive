package com.driver.app

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class Offer(val valor: Double, val distancia: Double, val classification: String, val timestamp: Long) {
    fun toJson(): JSONObject = JSONObject().apply { put("valor", valor); put("distancia", distancia); put("classification", classification); put("timestamp", timestamp) }
    companion object { fun fromJson(o: JSONObject): Offer = Offer(o.getDouble("valor"), o.getDouble("distancia"), o.getString("classification"), o.getLong("timestamp")) }
}

class MainActivity : AppCompatActivity() {
    private val PREFS = "driver_prefs"
    private val KEY_HISTORY = "offers_history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView = findViewById<ListView>(R.id.listView)
        val button = findViewById<Button>(R.id.buttonAdd)

        val offers = loadHistory().toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, offers.map { formatOffer(it) }.toMutableList())
        listView.adapter = adapter

        button.setOnClickListener {
            val newOffer = generateRandomOffer()
            offers.add(0, newOffer)
            saveHistory(offers)
            (listView.adapter as ArrayAdapter<String>).insert(formatOffer(newOffer), 0)
        }
    }

    private fun formatOffer(o: Offer): String = "R$%.2f — %.1f km — %s".format(o.valor, o.distancia, o.classification)

    private fun generateRandomOffer(): Offer {
        val valor = (Random.nextDouble(1.0, 30.0) * 100).toInt() / 100.0
        val distancia = (Random.nextDouble(0.5, 15.0) * 10).toInt() / 10.0
        val profit = valor - distancia * 0.6
        val classification = when { profit < 0 -> "VERMELHO"; profit > 5 -> "VERDE"; else -> "AMARELO" }
        return Offer(valor, distancia, classification, System.currentTimeMillis())
    }

    private fun saveHistory(list: List<Offer>) {
        val arr = JSONArray()
        for (o in list) arr.put(o.toJson())
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun loadHistory(): List<Offer> {
        val s = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, null) ?: return emptyList()
        val arr = JSONArray(s)
        val out = mutableListOf<Offer>()
        for (i in 0 until arr.length()) out.add(Offer.fromJson(arr.getJSONObject(i)))
        return out
    }
}
