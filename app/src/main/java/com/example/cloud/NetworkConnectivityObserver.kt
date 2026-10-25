package com.example.cloud

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Monitors real-time internet connectivity using Android's ConnectivityManager.NetworkCallback
 * so the UI and Smart Hybrid Sync engine know immediately when the device switches between
 * Online (Cloud Firestore) and Offline (Ultrasonic Acoustic NFC).
 */
class NetworkConnectivityObserver(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkLabelAr = MutableStateFlow(resolveNetworkLabelAr())
    val networkLabelAr: StateFlow<String> = _networkLabelAr.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateConnectivityState()
        }

        override fun onLost(network: Network) {
            updateConnectivityState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateConnectivityState()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Throwable) {
        }
        updateConnectivityState()
    }

    fun updateConnectivityState() {
        val online = checkCurrentlyOnline()
        _isOnline.value = online
        _networkLabelAr.value = resolveNetworkLabelAr()
    }

    private fun checkCurrentlyOnline(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveNetworkLabelAr(): String {
        return try {
            val cm = connectivityManager ?: return "أوفلاين • الموجات الصوتية"
            val activeNetwork = cm.activeNetwork ?: return "أوفلاين • الموجات الصوتية"
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "أوفلاين • الموجات الصوتية"
            when {
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ->
                    "أوفلاين • الموجات الصوتية"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    "متصل بالسحابة (Wi-Fi)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    "متصل بالسحابة (بيانات الهاتف)"
                else ->
                    "متصل بالسحابة (Online)"
            }
        } catch (_: Throwable) {
            "أوفلاين • الموجات الصوتية"
        }
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Throwable) {
        }
    }
}

object CloudNotificationHelper {
    private const val CHANNEL_ID = "vibrapay_cloud_notifications"
    private const val CHANNEL_NAME = "إشعارات محفظة VibraPay السحابية"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات التحويلات المالية الفورية السحابية"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun notifyIncomingCloudTransfer(
        context: Context,
        senderQuadName: String,
        amountFormatted: String,
        currencyCode: String,
        ottId: String
    ) {
        try {
            ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("استلام تحويل مالي سحابي فوري ✅")
                .setContentText("تم إضافة $amountFormatted $currencyCode إلى رصيدك من $senderQuadName (OTT #$ottId)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(Random.nextInt(10000, 99999), notification)
        } catch (_: Throwable) {
        }
    }
}
