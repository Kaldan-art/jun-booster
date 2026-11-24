
package com.junbooster

import android.net.VpnService
import android.content.Intent
import android.os.ParcelFileDescriptor

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (vpnInterface != null) {
            return START_STICKY
        }

        val builder = Builder()
            .setSession("JUN BOOSTER")
            .addAddress("10.10.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("1.1.1.1", 32)
            .setMtu(1500)

        vpnInterface = builder.establish()

        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
