package com.example.floodingradar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Broadcast silencioso para o Mapa atualizar em tempo real, independentemente do usuário ou distância!
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("UPDATE_ALERTS"))

        // Lógica de Notificação baseada no Data Payload
        if (remoteMessage.data.isNotEmpty()) {
            val tipo = remoteMessage.data["tipo_alerta"] ?: "Alerta"
            val autor = remoteMessage.data["usuario"] ?: "Alguém"
            val latStr = remoteMessage.data["latitude"]
            val lngStr = remoteMessage.data["longitude"]

            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val nomeUsuarioLogado = sharedPref.getString("nome_usuario", "")
            
            // 1. Autor não deve receber a notificação do seu próprio alerta
            if (autor == nomeUsuarioLogado) {
                return
            }

            // 2. Filtro de Raio
            val notificacoesRaioApenas = sharedPref.getBoolean("notificacoes_raio_apenas", true)
            if (notificacoesRaioApenas && latStr != null && lngStr != null) {
                val lastLat = sharedPref.getFloat("last_lat", 0f).toDouble()
                val lastLng = sharedPref.getFloat("last_lng", 0f).toDouble()

                if (lastLat != 0.0 && lastLng != 0.0) {
                    val alertLocation = Location("").apply {
                        latitude = latStr.toDoubleOrNull() ?: 0.0
                        longitude = lngStr.toDoubleOrNull() ?: 0.0
                    }
                    val userLocation = Location("").apply {
                        latitude = lastLat
                        longitude = lastLng
                    }

                    val distanceMeters = userLocation.distanceTo(alertLocation)
                    val limiteMeters = sharedPref.getInt("notificacoes_raio_km", 3) * 1000.0
                    if (distanceMeters > limiteMeters) {
                        return // Ignora a notificação pois está além do raio configurado
                    }
                }
            }

            val titulo = "⚠️ Novo Alerta na Região!"
            val mensagem = "$tipo reportado por $autor perto de você."
            mostrarNotificacao(titulo, mensagem, latStr, lngStr)
        }
    }

    private fun mostrarNotificacao(titulo: String, mensagem: String, latStr: String?, lngStr: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Envia a coordenada pro MainActivity navegar o mapa pro local
            if (latStr != null && lngStr != null) {
                putExtra("target_lat", latStr)
                putExtra("target_lng", lngStr)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "alertas_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Alagamento",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}

