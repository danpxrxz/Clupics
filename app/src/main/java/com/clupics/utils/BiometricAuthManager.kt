package com.clupics.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gestiona la autenticación biométrica o con PIN/patrón del sistema.
 *
 * Usa BIOMETRIC_STRONG | BIOMETRIC_WEAK | DEVICE_CREDENTIAL para cubrir:
 *   - Huella digital
 *   - Face unlock
 *   - PIN / patrón / contraseña del sistema
 */
object BiometricAuthManager {

    /**
     * Verifica si el dispositivo puede autenticarse con biometría o credencial de dispositivo.
     * Retorna true si hay al menos un método disponible y registrado.
     */
    fun isAvailable(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        val result = bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Lanza el prompt de autenticación.
     *
     * @param activity         FragmentActivity host (requerido por BiometricPrompt)
     * @param titleRes         Título del diálogo
     * @param subtitleRes      Subtítulo opcional
     * @param onSuccess        Callback cuando la autenticación es exitosa
     * @param onError          Callback cuando hay un error irrecuperable o el usuario cancela
     */
    fun authenticate(
        activity   : FragmentActivity,
        title      : String,
        subtitle   : String,
        onSuccess  : () -> Unit,
        onError    : (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Códigos que significan cancelación explícita del usuario — no mostramos error
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    onError("")
                } else {
                    onError(errString.toString())
                }
            }
            override fun onAuthenticationFailed() {
                // Intento fallido — BiometricPrompt lo maneja visualmente, no hacemos nada aquí
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            // Permite huella, face, y también PIN/patrón/contraseña como fallback
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}