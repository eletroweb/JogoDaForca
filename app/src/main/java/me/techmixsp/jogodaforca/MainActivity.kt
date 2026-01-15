package me.techmixsp.jogodaforca

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.techmixsp.jogodaforca.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()
    private var mediaPlayerAcerto: MediaPlayer? = null
    private var mediaPlayerErro: MediaPlayer? = null
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-2190295053343301/2295645715" // Id do anúncio correto

    // Variáveis do jogo
    private lateinit var palavraAtual: String
    private lateinit var palavraOculta: StringBuilder
    private val letrasErradas = mutableListOf<Char>()
    private var pontuacao = 0
    private var erros = 0
    private var vidas = 3
    private val maxErros = 6

    // Lista de palavras
    private val palavras = mutableListOf<String>()
    private val palavrasFiltradas = mutableListOf<String>()


    // IDs das imagens do boneco (de 0 a 6 erros)
    private val imagensForca = listOf(
        R.drawable.forca_vazia2,
        R.drawable.forca_cabeca2,
        R.drawable.forca_tronco2,
        R.drawable.forca_braco_esq2,
        R.drawable.forca_braco_dir2,
        R.drawable.forca_perna_esq2,
        R.drawable.forca_perna_dir2
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}
        loadRewardedAd()

        mediaPlayerAcerto = MediaPlayer.create(this, R.raw.acerto)
        mediaPlayerErro = MediaPlayer.create(this, R.raw.erro)

        carregarPalavras()
        configurarDificuldade()
        iniciarNovoJogo()
        criarTecladoVirtual()

        binding.btnNovoJogo.setOnClickListener {
            reiniciarJogoCompleto()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerAcerto?.release()
        mediaPlayerErro?.release()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedVideo() {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadRewardedAd()
                }

                override fun onAdShowedFullScreenContent() {
                    rewardedAd = null
                }
            }
            rewardedAd?.show(this) { rewardItem ->
                vidas++
                atualizarVidas()
                iniciarNovoJogo()
            }
        } else {
            Toast.makeText(this, "Anúncio não carregado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarPalavras() {
        try {
            val inputStream = resources.openRawResource(R.raw.palavras)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { palavras.add(it.uppercase()) }
            reader.close()
        } catch (e: Exception) {
            // Tratar erro de leitura do arquivo, se necessário
            palavras.addAll(listOf("ABACAXI", "BANANA", "COMPUTADOR")) // Palavras padrão
        }
    }

    private fun configurarDificuldade() {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        val difficulty = sharedPreferences.getInt("Difficulty", 0)

        palavrasFiltradas.clear()
        when (difficulty) {
            0 -> palavrasFiltradas.addAll(palavras.filter { it.length in 4..6 }) // Fácil
            1 -> palavrasFiltradas.addAll(palavras.filter { it.length in 7..9 }) // Médio
            2 -> palavrasFiltradas.addAll(palavras.filter { it.length >= 10 })    // Difícil
        }

        if (palavrasFiltradas.isEmpty()) {
            // Fallback para a lista completa se nenhuma palavra corresponder à dificuldade
            palavrasFiltradas.addAll(palavras)
        }
    }


    private fun iniciarNovoJogo() {
        if (vidas > 0) {
            if (palavrasFiltradas.isNotEmpty()) {
                palavraAtual = palavrasFiltradas.random()
                palavraOculta = StringBuilder("_".repeat(palavraAtual.length))

                letrasErradas.clear()
                erros = 0

                atualizarPalavraOculta()
                atualizarImagemForca()
                atualizarLetrasErradas()
                atualizarVidas()

                reativarTeclado()
            } else {
                mostrarMensagem("Nenhuma palavra encontrada para a dificuldade selecionada!")
            }
        } else {
            mostrarDialogoFimDeJogo()
        }
    }

    private fun reiniciarJogoCompleto() {
        vidas = 3
        pontuacao = 0
        binding.tvPontuacao.text = "Pontuação: $pontuacao"
        iniciarNovoJogo()
    }

    private fun criarTecladoVirtual() {
        binding.gridTeclado.removeAllViews()

        val letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        for (letra in letras) {
            val button = Button(this).apply {
                text = letra.toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    verificarLetra(letra)
                    isEnabled = false
                }
            }
            binding.gridTeclado.addView(button)
        }
    }

    private fun verificarLetra(letra: Char) {
        var acertou = false

        for (i in palavraAtual.indices) {
            if (palavraAtual[i].equals(letra, true)) {
                palavraOculta[i] = palavraAtual[i]
                acertou = true
            }
        }

        if (acertou) {
            mediaPlayerAcerto?.start()
            animarAcerto()
            atualizarPalavraOculta()

            if (!palavraOculta.contains('_')) {
                pontuacao += 10
                binding.tvPontuacao.text = "Pontuação: $pontuacao"
                mostrarMensagem("Parabéns! Você acertou! +10 pontos")
                salvarPontuacao("Player", pontuacao)
                Handler(Looper.getMainLooper()).postDelayed({ iniciarNovoJogo() }, 2000)
            }
        } else {
            mediaPlayerErro?.start()
            animarErro()
            letrasErradas.add(letra)
            erros++
            atualizarImagemForca()
            atualizarLetrasErradas()

            if (erros >= maxErros) {
                vidas--
                if (vidas > 0) {
                    mostrarMensagem("Você errou! A palavra era: $palavraAtual")
                    Handler(Looper.getMainLooper()).postDelayed({ iniciarNovoJogo() }, 2000)
                } else {
                    perderJogo()
                }
            }
        }
    }

    private fun atualizarPalavraOculta() {
        binding.tvPalavra.text = palavraOculta.toList().joinToString(" ")
    }

    private fun atualizarImagemForca() {
        binding.ivForca.setImageResource(imagensForca[erros])
    }

    private fun atualizarLetrasErradas() {
        binding.tvErros.text = "Erradas: ${letrasErradas.joinToString(", ")}"
    }

    private fun atualizarVidas() {
        binding.tvVidas.text = "Vidas: $vidas"
    }

    private fun perderJogo() {
        salvarPontuacao("Player", pontuacao)
        mostrarDialogoFimDeJogo()
        desativarTeclado()
    }

    private fun mostrarDialogoFimDeJogo() {
        AlertDialog.Builder(this)
            .setTitle("Fim de Jogo")
            .setMessage("Você não tem mais vidas. Deseja assistir a um anúncio para ganhar uma vida extra?")
            .setPositiveButton("Sim") { _, _ ->
                showRewardedVideo()
            }
            .setNegativeButton("Não") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun reativarTeclado() {
        for (i in 0 until binding.gridTeclado.childCount) {
            val button = binding.gridTeclado.getChildAt(i) as Button
            button.isEnabled = true
        }
    }

    private fun desativarTeclado() {
        for (i in 0 until binding.gridTeclado.childCount) {
            val button = binding.gridTeclado.getChildAt(i) as Button
            button.isEnabled = false
        }
    }

    private fun mostrarMensagem(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }

    private fun salvarPontuacao(playerName: String, score: Int) {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        val rankingJson = sharedPreferences.getString("Ranking", null)
        val scores = if (rankingJson != null) {
            val type = object : TypeToken<MutableList<Score>>() {}.type
            gson.fromJson<MutableList<Score>>(rankingJson, type)
        } else {
            mutableListOf()
        }

        scores.add(Score(playerName, score))
        scores.sortByDescending { it.score }

        val newRankingJson = gson.toJson(scores.take(10))
        sharedPreferences.edit().putString("Ranking", newRankingJson).apply()
    }

    private fun animarAcerto() {
        ObjectAnimator.ofFloat(binding.tvPalavra, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun animarErro() {
        ObjectAnimator.ofFloat(binding.ivForca, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
