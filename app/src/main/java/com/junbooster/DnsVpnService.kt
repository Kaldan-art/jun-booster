
package com.junbooster

import android.net.VpnService
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.Build
import android.content.Context
import android.content.pm.ServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.RejectedExecutionException
class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetThread: Thread? = null
private var dnsExecutor: ExecutorService? = null
    private val dohUrl =
    
        "https://quiet-art-35be.kaldanone2.workers.dev/doh-kali-7c9e2f4a1b6d8c3e5f0a9b2d4c6e8f1a/dns-query"

    private val dnsMediaType =
        "application/dns-message".toMediaType()

private val channelId = "jun_booster_vpn"
private val notificationId = 1001

private fun createVpnNotification(): Notification {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "JUN BOOSTER VPN",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        manager.createNotificationChannel(channel)
    }

    val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, channelId)
    } else {
        Notification.Builder(this)
    }

    return builder
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle("JUN BOOSTER")
        .setContentText("VPN service is running")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
}

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    startForeground(
        notificationId,
        createVpnNotification(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
    )
} else {
    startForeground(
        notificationId,
        createVpnNotification()
    )
}


        if (vpnInterface != null) return START_NOT_STICKY

        try {
            // Resolve the Worker before creating the VPN.
            val workerHost =
                "quiet-art-35be.kaldanone2.workers.dev"

            val workerAddresses =
                InetAddress.getAllByName(workerHost).toList()

            val client = OkHttpClient.Builder()
                .dns(object : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return if (hostname == workerHost) {
            workerAddresses
        } else {
            Dns.SYSTEM.lookup(hostname)
        }
    }
})
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val builder = Builder()
    .setSession("JUN BOOSTER")
    .addAddress("10.10.0.2", 32)
    .addDnsServer("10.10.0.1")
.addRoute("10.10.0.1", 32)
    .setMtu(1500)

            vpnInterface = builder.establish()

            val descriptor = vpnInterface ?: run {
                stopSelf()
                return START_NOT_STICKY
            }
            dnsExecutor = ThreadPoolExecutor(
    4,
    4,
    0L,
    TimeUnit.MILLISECONDS,
    ArrayBlockingQueue(64)
)
            packetThread = Thread {
                val input = FileInputStream(descriptor.fileDescriptor)
                val output = FileOutputStream(descriptor.fileDescriptor)
                val buffer = ByteArray(32767)

                try {
                    while (!Thread.currentThread().isInterrupted) {
                        val length = input.read(buffer)
                        if (length <= 0) continue

                        val packet = buffer.copyOf(length)

try {
    dnsExecutor?.execute {
        val response = forwardDns(packet, client)

        if (response != null) {
            try {
                synchronized(output) {
                    output.write(response)
                }
            } catch (_: Exception) {
                // VPN may have stopped.
            }
        }
    }
} catch (_: RejectedExecutionException) {
    // Queue is full; drop this DNS request.
}
                    }
                } catch (_: Exception) {
                    // The VPN may close while the service stops.
                }
            }.apply {
                name = "JUN-DNS-ENGINE"
                start()
            }

        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun forwardDns(
        packet: ByteArray,
        client: OkHttpClient
    ): ByteArray? {

        if (packet.size < 28) return null
        if ((packet[0].toInt() and 0xF0) != 0x40) return null

        val ipHeaderLength =
            (packet[0].toInt() and 0x0F) * 4

        if (ipHeaderLength < 20 || packet.size < ipHeaderLength + 8) {
            return null
        }

        if ((packet[9].toInt() and 0xFF) != 17) return null

        val totalLength =
            ((packet[2].toInt() and 0xFF) shl 8) or
            (packet[3].toInt() and 0xFF)

        if (totalLength > packet.size ||
            totalLength < ipHeaderLength + 8) return null

        val sourcePort =
            ((packet[ipHeaderLength].toInt() and 0xFF) shl 8) or
            (packet[ipHeaderLength + 1].toInt() and 0xFF)

        val destinationPort =
            ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
            (packet[ipHeaderLength + 3].toInt() and 0xFF)

        if (destinationPort != 53) return null

val udpLength =
    ((packet[ipHeaderLength + 4].toInt() and 0xFF) shl 8) or
    (packet[ipHeaderLength + 5].toInt() and 0xFF)

if (udpLength < 20 ||
    udpLength > totalLength - ipHeaderLength) return null

val dnsStart = ipHeaderLength + 8
val dnsLength = udpLength - 8
if (dnsLength < 12) return null

        val dnsQuery = packet.copyOfRange(
            dnsStart,
            dnsStart + dnsLength
        )

        val request = Request.Builder()
            .url(dohUrl)
            .header("Accept", "application/dns-message")
            .post(dnsQuery.toRequestBody(dnsMediaType))
            .build()

        val dnsResponse = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes()
            }
        } catch (_: Exception) {
            null
        } ?: return null

        if (dnsResponse.size < 12) return null
        
        if (dnsResponse[0] != dnsQuery[0] ||
    dnsResponse[1] != dnsQuery[1]) return null

        return buildUdpResponse(
            packet,
            ipHeaderLength,
            sourcePort,
            dnsResponse
        )
    }

    private fun buildUdpResponse(
        original: ByteArray,
        ipHeaderLength: Int,
        sourcePort: Int,
        dns: ByteArray
    ): ByteArray {

        val result = ByteArray(20 + 8 + dns.size)

        // IPv4 header
        result[0] = 0x45
        result[1] = 0

        put16(result, 2, result.size)
        result[4] = original[4]
        result[5] = original[5]

        result[6] = 0x40
        result[7] = 0
        result[8] = 64
        result[9] = 17

        // Swap source and destination IP addresses.
        System.arraycopy(original, 16, result, 12, 4)
        System.arraycopy(original, 12, result, 16, 4)

        // UDP header
        put16(result, 20, 53)
        put16(result, 22, sourcePort)
        put16(result, 24, 8 + dns.size)
        put16(result, 26, 0)

        System.arraycopy(dns, 0, result, 28, dns.size)

        put16(result, 10, checksum(result, 0, 20))

        return result
    }

    private fun put16(
        data: ByteArray,
        offset: Int,
        value: Int
    ) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    private fun checksum(
        data: ByteArray,
        offset: Int,
        length: Int
    ): Int {

        var sum = 0
        var i = offset

        while (i < offset + length) {
            val high = data[i].toInt() and 0xFF
            val low = if (i + 1 < offset + length) {
                data[i + 1].toInt() and 0xFF
            } else {
                0
            }

            sum += (high shl 8) or low
            i += 2
        }

        while ((sum shr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return sum.inv() and 0xFFFF
    }
override fun onRevoke() {
    stopSelf()
    super.onRevoke()
}

override fun onDestroy() {
    dnsExecutor?.shutdownNow()
dnsExecutor = null
    
    packetThread?.interrupt()

    try {
        vpnInterface?.close()
    } catch (_: Exception) {
    }

    vpnInterface = null
    packetThread = null

    stopForeground(true)
    super.onDestroy()
} 
}
