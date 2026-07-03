package com.erekeai.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.erekeai.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Foreground-сервис, держащий [ErekeApiServer] (REST-ядро для других приложений) запущенным. */
@AndroidEntryPoint
class ErekeApiForegroundService : Service() {

    @Inject lateinit var apiServer: ErekeApiServer

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        apiServer.start(30_000, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_AUTH_TOKEN)?.let { apiServer.authToken = it.ifBlank { null } }
        return START_STICKY
    }

    override fun onDestroy() { apiServer.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "ereke_rest_api_server"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(channelId, "ErekeAI REST API", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ErekeAI REST API активен")
            .setContentText("Порт ${ErekeApiServer.PORT} — SDK/другие приложения могут использовать ErekeAI как ядро")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 44
        const val EXTRA_AUTH_TOKEN = "auth_token"
    }
}
