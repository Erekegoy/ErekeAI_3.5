package com.erekeai.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.erekeai.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Foreground-сервис, держащий [ErekeMcpServer] запущенным, пока пользователь явно его не выключил. */
@AndroidEntryPoint
class ErekeMcpServerService : Service() {

    @Inject lateinit var mcpServer: ErekeMcpServer

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        mcpServer.start(30_000, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { mcpServer.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "ereke_mcp_server"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(channelId, "ErekeAI MCP-сервер", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ErekeAI MCP-сервер активен")
            .setContentText("Порт ${ErekeMcpServer.PORT} — другие MCP-клиенты могут подключиться и использовать инструменты ErekeAI")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object { private const val NOTIFICATION_ID = 43 }
}
