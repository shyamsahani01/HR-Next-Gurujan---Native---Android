package com.example.hrnext.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.hrnext.HRNextApp
import com.example.hrnext.MainActivity
import com.example.hrnext.R
import com.example.hrnext.data.CheckinRepository
import com.example.hrnext.data.DocRepository
import com.example.hrnext.location.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs only while the current employee is checked in: keeps a persistent notification up and,
 * every 2 minutes, records a fresh location by creating another `Employee Checkin` (log_type
 * "IN") for the current employee — reusing the same doctype/endpoint as the check-in button.
 * Never restarted by the platform ([START_NOT_STICKY]); resumption after process death is driven
 * only by Home re-issuing [ContextCompat.startForegroundService] once it reconciles with server truth.
 */
class CheckinLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        // onStartCommand can be re-issued (e.g. Home re-verifying on every app open) while a ping
        // loop is already running — must not spawn a second one.
        if (pingJob?.isActive != true) {
            pingJob = scope.launch {
                while (true) {
                    delay(PING_INTERVAL_MS)
                    ping()
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun ping() {
        val container = (application as HRNextApp).container
        val session = container.sessionManager.sessionFlow.first()
        val employeeId = session?.employeeId
        if (session == null || employeeId == null) {
            stopSelf()
            return
        }
        val checkinRepository = CheckinRepository(DocRepository(container.apiFor(session.siteUrl)))
        val location = LocationProvider.getCurrentLocation(this)
        checkinRepository.createCheckin(employeeId, "IN", location)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Checked in")
            .setContentText("Location tracking is on")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Check-in tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "checkin_tracking"
        const val NOTIFICATION_ID = 1001
        const val PING_INTERVAL_MS = 2 * 60 * 1000L
    }
}
