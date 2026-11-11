package com.mapfre.autoanswer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * Telefonun arama durumu değiştiğinde çalışır.
 * Hedef numara değilse HİÇBİR ŞEY yapmaz; arama telefonun kendi uygulamasından normal gelir.
 */
class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val prefs = Prefs(context)
        if (!prefs.enabled) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        @Suppress("DEPRECATION")
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Android bu yayını iki kez gönderir: biri numarasız, biri numaralı.
                if (number != null && prefs.matches(number)) {
                    CallAutomator.onTargetRinging(context.applicationContext)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> CallAutomator.onOffhook(context.applicationContext)
            TelephonyManager.EXTRA_STATE_IDLE -> CallAutomator.onIdle(context.applicationContext)
        }
    }
}
