package me.techmixsp.jogodaforca

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.techmixsp.jogodaforca.databinding.ActivityRankingBinding

class RankingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRankingBinding
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayRanking()

        binding.btnClearRanking.setOnClickListener {
            clearRanking()
            displayRanking() // Refresh the list
        }
    }

    private fun displayRanking() {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        val rankingJson = sharedPreferences.getString("Ranking", null)
        val scores = if (rankingJson != null) {
            val type = object : TypeToken<MutableList<Score>>() {}.type
            gson.fromJson<MutableList<Score>>(rankingJson, type)
        } else {
            mutableListOf()
        }

        scores.sortByDescending { it.score }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scores.map { "${it.playerName}: ${it.score}" })
        binding.lvRanking.adapter = adapter
    }

    private fun clearRanking() {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("Ranking").apply()
    }
}