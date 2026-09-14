package com.example.floodingradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.floodingradar.ui.screens.map.MapScreen

import androidx.activity.viewModels
import com.example.floodingradar.viewmodel.MapViewModel
import com.google.firebase.messaging.FirebaseMessaging

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.floodingradar.ui.screens.home.HomeScreen

import com.example.floodingradar.ui.screens.reports.ReportsScreen
import com.example.floodingradar.ui.screens.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    private val mapViewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPref = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val nomeUsuario = sharedPref.getString("nome_usuario", "")
        val notificacoesAtivadas = sharedPref.getBoolean("notificacoes_ativadas", true)
        val startDest = if (!nomeUsuario.isNullOrBlank()) "map" else "home"

        // Inscreve no tópico apenas se o usuário permitiu nas configurações
        if (notificacoesAtivadas) {
            FirebaseMessaging.getInstance().subscribeToTopic("alertas")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alertas")
        }

        // Pede permissão de Notificação no Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("home") {
                            HomeScreen(onNavigateToMap = {
                                navController.navigate("map") {
                                    popUpTo("home") { inclusive = true }
                                }
                            })
                        }
                        composable("map") {
                            MapScreen(
                                viewModel = mapViewModel,
                                onNavigateToReports = { navController.navigate("reports") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("reports") {
                            ReportsScreen(
                                viewModel = mapViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

