package com.stillfeed.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stillfeed.app.core.navigation.StillfeedNavHost
import com.stillfeed.app.core.ui.theme.StillfeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            StillfeedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    StillfeedNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.data?.toString()
        if (!uri.isNullOrBlank()) {
            (application as? StillfeedApplication)?.pendingDeepLink?.value = uri
        }
    }
}
