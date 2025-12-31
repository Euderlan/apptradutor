package com.example.texttranslatorapp.presentation.utils

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.example.texttranslatorapp.presentation.viewmodel.TranslatorViewModel

class TextWatcherDebounceManager(
    private val viewModel: TranslatorViewModel,
    // Tempo de espera antes de propagar a mudança de texto (debounce)
    private val debounceDelay: Long = 500L
) {

    // Handler associado à main thread para agendar execução atrasada
    private val handler = Handler(Looper.getMainLooper())
    private var textWatcherRunnable: Runnable? = null

    // Flag usada para evitar disparo do watcher quando o texto é atualizado via UI/ViewModel
    private var isUpdatingUI = false

    fun setupTextWatcher(textoExtraido: EditText) {
        textoExtraido.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Só reage a mudanças do usuário (não da UI programática)
                if (!isUpdatingUI && s != null) {
                    // Remove callback anterior para aplicar o debounce
                    textWatcherRunnable?.let { handler.removeCallbacks(it) }

                    textWatcherRunnable = Runnable {
                        // Atualiza o texto no ViewModel após o atraso configurado
                        val textoAtual = s.toString()
                        viewModel.updateExtractedText(textoAtual)
                    }

                    handler.postDelayed(textWatcherRunnable!!, debounceDelay)
                }
            }
        })
    }

    // Controla se o watcher deve ignorar atualizações vindas da UI
    fun setUpdatingUI(updating: Boolean) {
        isUpdatingUI = updating
    }

    // Remove callbacks pendentes para evitar vazamento de memória
    fun cleanup() {
        textWatcherRunnable?.let { handler.removeCallbacks(it) }
    }
}
