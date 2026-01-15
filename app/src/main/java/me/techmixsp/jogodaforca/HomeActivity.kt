package me.techmixsp.jogodaforca

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import me.techmixsp.jogodaforca.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)

        binding.btnNewGame.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnDifficulty.setOnClickListener {
            showDifficultyDialog()
        }

        binding.btnRanking.setOnClickListener {
            startActivity(Intent(this, RankingActivity::class.java))
        }

        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        binding.switchTheme.isChecked = isDarkMode
        updateTheme(isDarkMode)

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("DarkMode", isChecked).apply()
            updateTheme(isChecked)
        }
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("Fácil", "Médio", "Difícil")
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        val currentDifficulty = sharedPreferences.getInt("Difficulty", 0) // 0: Fácil, 1: Médio, 2: Difícil

        AlertDialog.Builder(this)
            .setTitle("Escolha a Dificuldade")
            .setSingleChoiceItems(difficulties, currentDifficulty) { dialog, which ->
                sharedPreferences.edit().putInt("Difficulty", which).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}