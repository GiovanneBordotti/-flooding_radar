package com.example.floodingradar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.floodingradar.ui.screens.map.MapScreen
import androidx.activity.viewModels
import com.example.floodingradar.viewmodel.MapViewModel
import com.google.firebase.messaging.FirebaseMessaging
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.floodingradar.ui.screens.home.HomeScreen
import com.example.floodingradar.ui.screens.home.OnboardingScreen
import com.example.floodingradar.ui.screens.reports.ReportsScreen
import com.example.floodingradar.ui.screens.settings.SettingsScreen
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    private val mapViewModel: MapViewModel by viewModels()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Atualiza os alertas do mapa automaticamente quando recebe um push
            mapViewModel.buscarAlertas()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Ignora, Firebase já cuida do resto se for garantido
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Registra para escutar atualizações em tempo real
        LocalBroadcastManager.getInstance(this).registerReceiver(
            refreshReceiver, IntentFilter("UPDATE_ALERTS")
        )

        val sharedPref = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val nomeUsuario = sharedPref.getString("nome_usuario", "")
        val notificacoesAtivadas = sharedPref.getBoolean("notificacoes_ativadas", true)
        val aceitouTermos = sharedPref.getBoolean("aceitou_termos", false)
        val startDest = if (!nomeUsuario.isNullOrBlank() && aceitouTermos) "home" else "onboarding"

        if (notificacoesAtivadas) {
            FirebaseMessaging.getInstance().subscribeToTopic("alertas")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alertas")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Pega as coordenadas se tiver sido aberto clicando numa Notificação
        val targetLat = intent.getStringExtra("target_lat")
        val targetLng = intent.getStringExtra("target_lng")

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute == "home" || currentRoute == "map") {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Place, contentDescription = "Mapa") },
                                    label = { Text("Mapa") },
                                    selected = currentRoute == "map",
                                    onClick = {
                                        if (currentRoute != "map") {
                                            navController.navigate("map") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("onboarding") {
                                OnboardingScreen(onNavigateToMap = {
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                })
                            }
                            composable("home") {
                                HomeScreen(onNavigateToMap = {
                                    navController.navigate("map")
                                })
                            }
                            composable("map") {
                                MapScreen(
                                    viewModel = mapViewModel,
                                    targetLat = targetLat,
                                    targetLng = targetLng,
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

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshReceiver)
    }
}

