package com.endrunners.capri

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.random.Random

class CapriService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var capriContainer: LinearLayout
    private lateinit var capriSprite: ImageView
    private lateinit var speechBubble: TextView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())
    private var isWalking = true
    private var directionX = 4

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        tts = TextToSpeech(this, this)
        setupFloatingCharacter()
        startAutonomousBehavior()
    }

    private fun startForegroundNotification() {
        val channelId = "capri_core_channel"
        val channel = NotificationChannel(channelId, "Capri Core", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Capri // End Runners OS")
            .setContentText("Sistema activo en pantalla")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun setupFloatingCharacter() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        capriContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        speechBubble = TextView(this).apply {
            setBackgroundColor(0xDD0D0D12.toInt())
            setTextColor(0xFF00F0FF.toInt())
            setPadding(24, 14, 24, 14)
            textSize = 12f
            visibility = View.GONE
        }

        capriSprite = ImageView(this).apply {
            setImageResource(android.R.drawable.sym_def_app_icon)
            layoutParams = LinearLayout.LayoutParams(180, 180)
        }

        capriContainer.addView(speechBubble)
        capriContainer.addView(capriSprite)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 500
        }

        setupInteractions()
        windowManager.addView(capriContainer, params)
    }

    private fun setupInteractions() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        capriSprite.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isWalking = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(capriContainer, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        onCapriTapped()
                    }
                    handler.postDelayed({ isWalking = true }, 4000)
                    true
                }
                else -> false
            }
        }
    }

    private fun onCapriTapped() {
        val frases = listOf(
            "Sistema en línea. ¿Qué frecuencia ponemos hoy?",
            "Ojo con ese movimiento, corredor. ¿Necesitas un tip?",
            "Capri escuchando. Dime qué quieres controlar.",
            "Energía al 100%. ¿Subimos el bass en Spotify?"
        )
        speakAndShow(frases.random())
    }

    private fun startAutonomousBehavior() {
        val screenWidth = resources.displayMetrics.widthPixels

        handler.post(object : Runnable {
            override fun run() {
                if (isWalking) {
                    params.x += directionX
                    if (params.x > screenWidth - 200 || params.x < 0) {
                        directionX = -directionX
                        capriSprite.scaleX = if (directionX > 0) 1f else -1f
                    }
                    windowManager.updateViewLayout(capriContainer, params)

                    if (Random.nextInt(100) < 2) {
                        isWalking = false
                        handler.postDelayed({ isWalking = true }, 3000)
                    }
                }
                handler.postDelayed(this, 35)
            }
        })
    }

    fun speakAndShow(text: String) {
        speechBubble.text = text
        speechBubble.visibility = View.VISIBLE
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "capri_utterance")

        handler.removeCallbacksAndMessages("hide_bubble")
        handler.postAtTime({
            speechBubble.visibility = View.GONE
        }, "hide_bubble", SystemClock.uptimeMillis() + 5000)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
            tts.setPitch(0.95f)
            tts.setSpeechRate(1.08f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        windowManager.removeView(capriContainer)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
