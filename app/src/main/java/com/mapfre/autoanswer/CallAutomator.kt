package com.mapfre.autoanswer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/**
 * Sadece Mapfre robotu aradığında devreye giren akış:
 * 1) Aramayı sistem komutuyla aç (kaydırmaya gerek yok).
 * 2) Açılmazsa ekrandaki "Cevapla" düğmesine bas, o da olmazsa kaydır.
 * 3) Arama bağlanınca bekle ve # tuşuna bas.
 */
object CallAutomator {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var active = false
    @Volatile private var hashScheduled = false

    fun onTargetRinging(ctx: Context) {
        if (active) return
        active = true
        hashScheduled = false
        val prefs = Prefs(ctx)
        prefs.log("Mapfre araması geldi, açılıyor")
        if (AutoAccessibilityService.instance == null) {
            prefs.log("UYARI: Erişilebilirlik servisi kapalı, # basılamaz")
        }

        // 1) Sistem komutuyla aç
        handler.postDelayed({ acceptBySystem(ctx) }, 400)

        // 2) Hâlâ çalıyorsa ekrandaki düğmeye bas
        handler.postDelayed({
            if (active && isRinging(ctx)) {
                prefs.log("Sistem komutu açmadı, 'Cevapla' düğmesi deneniyor")
                AutoAccessibilityService.instance?.tapAnswerButton()
            }
        }, 2500)

        // 3) Hâlâ çalıyorsa kaydır
        handler.postDelayed({
            if (active && isRinging(ctx)) {
                prefs.log("Düğme bulunamadı, kaydırma yapılıyor")
                AutoAccessibilityService.instance?.swipeToAnswer()
            }
        }, 4500)
    }

    fun onOffhook(ctx: Context) {
        if (!active || hashScheduled) return
        hashScheduled = true
        val prefs = Prefs(ctx)
        prefs.log("Arama açıldı, ${prefs.hashDelaySec} sn sonra # basılacak")
        handler.postDelayed({
            if (active) AutoAccessibilityService.instance?.pressHash(attempt = 0)
                ?: prefs.log("HATA: Erişilebilirlik servisi kapalı")
        }, prefs.hashDelaySec * 1000L)
    }

    fun onIdle(ctx: Context) {
        if (active) Prefs(ctx).log("Arama bitti")
        active = false
        hashScheduled = false
        handler.removeCallbacksAndMessages(null)
    }

    fun isActive() = active

    private fun acceptBySystem(ctx: Context) {
        if (ctx.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            Prefs(ctx).log("ANSWER_PHONE_CALLS izni yok")
            return
        }
        try {
            val tm = ctx.getSystemService(TelecomManager::class.java)
            @Suppress("DEPRECATION")
            tm.acceptRingingCall()
        } catch (e: Exception) {
            Prefs(ctx).log("Sistem ile açma hatası: ${e.message}")
        }
    }

    private fun isRinging(ctx: Context): Boolean = try {
        @Suppress("DEPRECATION")
        ctx.getSystemService(TelephonyManager::class.java).callState == TelephonyManager.CALL_STATE_RINGING
    } catch (e: SecurityException) {
        false
    }
}
