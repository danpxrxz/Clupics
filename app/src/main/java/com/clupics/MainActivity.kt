package com.clupics

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clupics.ui.GaleriaApp
import com.clupics.ui.settings.AppLanguage
import com.clupics.ui.settings.SettingsViewModel
import com.clupics.ui.theme.GaleriaTheme
import com.clupics.utils.BiometricAuthManager
import com.google.android.gms.cast.framework.CastContext

class MainActivity : AppCompatActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private val _unlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        // Edge-to-edge manual — NO usamos enableEdgeToEdge() de AndroidX porque
        // esa función aplica scrims automáticos que no podemos controlar y generan
        // la franja de color en la status bar. Hacemos lo mismo pero sin scrims.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        try { CastContext.getSharedInstance(this) } catch (_: Exception) {}

        setContent {
            val settings  by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            LaunchedEffect(settings.language) { applyLocale(settings.language) }

            val isDark   = settings.darkModeOverride ?: systemDark
            val isAmoled = settings.amoledMode && isDark

            GaleriaTheme(
                darkTheme    = isDark,
                amoledTheme  = isAmoled,
                dynamicColor = settings.dynamicColor
            ) {
                // Sin Surface de fondo — el fondo lo pone el windowBackground del tema
                // y Compose dibuja directamente encima. Esto evita que Material3
                // calcule el "tonal elevation" del TopAppBar contra un Surface y
                // cambie su color al hacer scroll.
                var unlocked by remember { _unlocked }

                if (settings.appLockEnabled && !unlocked) {
                    LockScreen(
                        onUnlock = {
                            BiometricAuthManager.authenticate(
                                activity  = this@MainActivity,
                                title     = getString(R.string.lock_title),
                                subtitle  = getString(R.string.lock_subtitle),
                                onSuccess = { unlocked = true },
                                onError   = {}
                            )
                        }
                    )
                    LaunchedEffect(Unit) {
                        BiometricAuthManager.authenticate(
                            activity  = this@MainActivity,
                            title     = getString(R.string.lock_title),
                            subtitle  = getString(R.string.lock_subtitle),
                            onSuccess = { unlocked = true },
                            onError   = {}
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = unlocked || !settings.appLockEnabled,
                        enter   = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.97f),
                        exit    = fadeOut(tween(200))
                    ) {
                        GaleriaApp(settingsViewModel = settingsViewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val lockEnabled = settingsViewModel.state.value.appLockEnabled
        if (lockEnabled) _unlocked.value = false
    }

    private fun applyLocale(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM)
            LocaleListCompat.getEmptyLocaleList()
        else
            LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}

@androidx.compose.runtime.Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.Lock,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text       = "Clupics",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = androidx.compose.ui.res.stringResource(R.string.lock_message),
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 48.dp)
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onUnlock,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector        = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text  = androidx.compose.ui.res.stringResource(R.string.lock_unlock_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
