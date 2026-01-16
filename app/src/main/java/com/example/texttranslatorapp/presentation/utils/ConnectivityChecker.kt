package com.example.texttranslatorapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utilitário responsável por verificar o estado real de conectividade do dispositivo.
 *
 * Não verifica apenas se há uma rede ativa, mas se ela possui acesso válido à internet.
 */
class ConnectivityChecker(private val context: Context) {

    /**
     * Verifica se o dispositivo está efetivamente online.
     *
     * @return true se houver conexão com internet válida, false caso contrário.
     */
    fun isOnline(): Boolean {
        // Obtém o serviço de conectividade do sistema
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Retorna false se não houver nenhuma rede ativa
        val network = cm.activeNetwork ?: return false

        // Obtém as capacidades da rede ativa
        val caps = cm.getNetworkCapabilities(network) ?: return false

        // NET_CAPABILITY_INTERNET:
        // indica que a rede possui potencial de acesso à internet
        //
        // NET_CAPABILITY_VALIDATED:
        // garante que a rede realmente consegue acessar a internet
        // (evita casos de Wi-Fi conectado sem acesso externo)
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
