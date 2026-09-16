package com.example.floodingradar.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import com.example.floodingradar.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.Circle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    targetLat: String? = null,
    targetLng: String? = null,
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val alertas by viewModel.alertas.collectAsState()

    val todaySdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val todayStr = todaySdf.format(java.util.Date())
    val alertasHoje = alertas.filter { it.data_hora?.startsWith(todayStr) == true }

    var showOfflineDialog by remember { mutableStateOf(false) }
    var showNearbyDialog by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val saoPaulo = LatLng(-23.5505, -46.6333)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(saoPaulo, 12f)
    }

    var hasLocationPermission by remember { mutableStateOf(false) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (isGranted) {
                try {
                    fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                        location?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().apply {
                                putFloat("last_lat", it.latitude.toFloat())
                                putFloat("last_lng", it.longitude.toFloat())
                                apply()
                            }
                            // Apenas move para o usuário se NÃO tiver vindo da notificação
                            if (targetLat == null || targetLng == null) {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(it.latitude, it.longitude), 17f
                                )
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (targetLat != null && targetLng != null) {
            // Se veio da notificação, já centraliza o mapa no alerta reportado
            val lat = targetLat.toDoubleOrNull() ?: saoPaulo.latitude
            val lng = targetLng.toDoubleOrNull() ?: saoPaulo.longitude
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 17f)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                android.widget.Toast.makeText(context, "Por favor, ative o GPS (Localização) do seu celular.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().apply {
                            putFloat("last_lat", it.latitude.toFloat())
                            putFloat("last_lng", it.longitude.toFloat())
                            apply()
                        }
                        if (targetLat == null || targetLng == null) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(it.latitude, it.longitude), 17f
                            )
                        }
                    }
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        viewModel.buscarAlertas()
    }

    // Exibe Snackbars (mensagens pop-up) do ViewModel
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.mensagensUi.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Int?>(null) }
    var observacao by remember { mutableStateOf("") }
    var tipoOutro by remember { mutableStateOf("") }
    var isOutroSelected by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var reportLocation by remember { mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, 
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Mapa") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                val context = LocalContext.current
                val sharedPref = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
                val raioKm = sharedPref.getInt("notificacoes_raio_km", 3)
                
                NavigationDrawerItem(
                    label = { Text("Alertas próximos (${raioKm}km)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNearbyDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Relatório Histórico") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToReports()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Meus Alertas (Offline)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showOfflineDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Radar de Alagamentos") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            // Movemos o FAB para o centro/baixo, grande e moderno, assim não sobrepõe os botões de Zoom da direita
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { 
                        reportLocation = cameraPositionState.position.target
                        showBottomSheet = true 
                    },
                    icon = { Icon(Icons.Default.Add, "Reportar") },
                    text = { Text("Reportar Ocorrência") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    // Habilitando o zoom control novamente como o usuário pediu
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    // Coloca um padding na direita/baixo para o Logo do Google e Zoom subirem um pouco
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val usuarioLogado = sharedPref.getString("nome_usuario", "Anônimo") ?: "Anônimo"
                    val raioKm = sharedPref.getInt("notificacoes_raio_km", 3)

                    // Desenha o Radar (Círculo fixo e mais transparente para melhor performance)
                    userLocation?.let { loc ->
                        Circle(
                            center = loc,
                            radius = raioKm * 1000.0,
                            fillColor = androidx.compose.ui.graphics.Color(0x0A0000FF), // Azul bem fraquinho (4% opacidade)
                            strokeColor = androidx.compose.ui.graphics.Color(0x330000FF), // Borda sutil (20% opacidade)
                            strokeWidth = 2f
                        )
                    }

                    alertasHoje.filter { it.status != "removido" }.forEach { alerta ->
                        val dataFormatada = alerta.data_hora?.let { dh ->
                            try {
                                val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                val date = parser.parse(dh.substringBefore('.'))
                                val formatter = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                date?.let { formatter.format(it) }
                            } catch (e: Exception) { null }
                        } ?: ""

                        val obsStr = if (alerta.observacao.isNullOrBlank()) "" else " - ${alerta.observacao}"
                        val tempoStr = if (dataFormatada.isNotBlank()) " ($dataFormatada)" else ""

                        MarkerInfoWindowContent(
                            state = MarkerState(position = LatLng(alerta.latitude, alerta.longitude)),
                            title = alerta.tipo_alerta,
                            snippet = "${alerta.usuario}$tempoStr$obsStr",
                            onInfoWindowClick = {
                                if (alerta.usuario == usuarioLogado) {
                                    if (alerta.id != null) {
                                        showDeleteConfirmDialog = alerta.id
                                    } else {
                                        android.widget.Toast.makeText(context, "Sincronizando com o servidor... Tente excluir em instantes.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) { marker ->
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(marker.title ?: "", fontWeight = FontWeight.Bold)
                                    Text(marker.snippet ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                                if (alerta.usuario == usuarioLogado) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = androidx.compose.ui.graphics.Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("Excluir", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Alvo de Reporte",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (showDeleteConfirmDialog != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = null },
                    title = { Text("Excluir Alerta") },
                    text = { Text("Deseja realmente retirar este alerta do mapa?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.removerAlerta(showDeleteConfirmDialog!!)
                                showDeleteConfirmDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancelar") }
                    }
                )
            }

            if (showConfirmDialog != null) {
                val tipo = if (showConfirmDialog == "Outro") tipoOutro else showConfirmDialog!!
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = null },
                    title = { Text("Confirmar Alerta") },
                    text = { Text("Você tem certeza que deseja reportar '$tipo' neste local?") },
                    confirmButton = {
                        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val usuarioLogado = sharedPref.getString("nome_usuario", "Anônimo") ?: "Anônimo"
                        
                        Button(onClick = {
                            val target = reportLocation ?: cameraPositionState.position.target
                            viewModel.enviarAlerta(tipo, target.latitude, target.longitude, observacao, usuarioLogado)
                            showConfirmDialog = null
                            showBottomSheet = false
                            observacao = ""
                            tipoOutro = ""
                            isOutroSelected = false
                        }) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = null }) { Text("Cancelar") }
                    }
                )
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        showBottomSheet = false 
                        isOutroSelected = false
                        tipoOutro = ""
                    },
                    sheetState = sheetState
                ) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "O que você deseja reportar?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    OutlinedTextField(
                                        value = observacao,
                                        onValueChange = { observacao = it },
                                        label = { Text("Observação (Opcional)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ReportOptionItem(text = "Alagamento", color = MaterialTheme.colorScheme.primary) { showConfirmDialog = "Alagamento" }
                                    ReportOptionItem(text = "Árvore Caída", color = MaterialTheme.colorScheme.error) { showConfirmDialog = "Árvore Caída" }
                                    ReportOptionItem(text = "Trânsito / Bloqueio", color = MaterialTheme.colorScheme.tertiary) { showConfirmDialog = "Trânsito / Bloqueio" }
                                    
                                    if (isOutroSelected) {
                                        OutlinedTextField(
                                            value = tipoOutro,
                                            onValueChange = { tipoOutro = it },
                                            label = { Text("Qual o tipo de problema?") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Button(
                                            onClick = { 
                                                if (tipoOutro.isNotBlank()) showConfirmDialog = "Outro" 
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Reportar Problema")
                                        }
                                    } else {
                                        ReportOptionItem(text = "Outro", color = androidx.compose.ui.graphics.Color.Gray) { isOutroSelected = true }
                                    }
                                    
                                    Spacer(modifier = Modifier.padding(bottom = 32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNearbyDialog) {
        val myLoc = cameraPositionState.position.target 
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val raioKm = sharedPref.getInt("notificacoes_raio_km", 3)
        val raioMetros = raioKm * 1000f
        
        // Filtra os alertas em um raio dinâmico
        val proximos = alertasHoje.filter { a ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(myLoc.latitude, myLoc.longitude, a.latitude, a.longitude, results)
            results[0] <= raioMetros
        }.sortedBy { a ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(myLoc.latitude, myLoc.longitude, a.latitude, a.longitude, results)
            results[0]
        }

        AlertDialog(
            onDismissRequest = { showNearbyDialog = false },
            title = { Text("Alertas Num Raio de ${raioKm}km") },
            text = {
                if (proximos.isEmpty()) {
                    Text("Nenhum alerta reportado nos arredores (${raioKm}km).")
                } else {
                    LazyColumn {
                        items(proximos) { p ->
                            var rua by remember { mutableStateOf("Buscando endereço...") }
                            
                            LaunchedEffect(p) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale("pt", "BR"))
                                        val addresses = geocoder.getFromLocation(p.latitude, p.longitude, 1)
                                        rua = if (!addresses.isNullOrEmpty()) {
                                            addresses[0].thoroughfare ?: "Rua desconhecida"
                                        } else {
                                            "Rua desconhecida"
                                        }
                                    } catch (e: Exception) {
                                        rua = "Endereço indisponível"
                                    }
                                }
                            }

                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(myLoc.latitude, myLoc.longitude, p.latitude, p.longitude, results)
                            val distance = String.format("%.1f", results[0] / 1000)

                            val dataFormatada = p.data_hora?.let { dh ->
                                try {
                                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                    val date = parser.parse(dh.substringBefore('.'))
                                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    date?.let { formatter.format(it) } ?: "Sem data"
                                } catch (e: Exception) { "Data inválida" }
                            } ?: "Sem data"

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                val usuarioLogado = sharedPref.getString("nome_usuario", "Anônimo") ?: "Anônimo"

                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (p.status == "removido") {
                                        Text(text = "${p.tipo_alerta} [Retirado pelo autor]", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Gray)
                                        Text(text = "Em: $dataFormatada", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth().clickable {
    cameraPositionState.position = CameraPosition.fromLatLngZoom(
        LatLng(p.latitude, p.longitude), 17f
    )
    showNearbyDialog = false
}.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = p.tipo_alerta, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                if (!p.observacao.isNullOrBlank()) {
                                                    Text(text = "Obs: ${p.observacao}", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                }
                                                Text(text = "Rua: $rua", style = MaterialTheme.typography.bodyMedium)
                                                Text(text = "Distância: $distance km", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "Em: $dataFormatada", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "Por: ${p.usuario}", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                                            }
                                            if (p.usuario == usuarioLogado && p.id != null) {
                                                IconButton(onClick = { viewModel.removerAlerta(p.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remover Alerta", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNearbyDialog = false }) { Text("Fechar") }
            }
        )
    }

    if (showOfflineDialog) {
        val pendentes = alertas.filter { it.status == "pendente" }

        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { Text("Meus Alertas Offline") },
            text = {
                if (pendentes.isEmpty()) {
                    Text("Nenhum alerta pendente de sincronização. Tudo em dia!")
                } else {
                    Column {
                        Text("Estes alertas estão salvos localmente e aguardando conexão:")
                        Spacer(modifier = Modifier.height(8.dp))
                        pendentes.forEach { p ->
                            Text("- ${p.tipo_alerta}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
fun ReportOptionItem(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
