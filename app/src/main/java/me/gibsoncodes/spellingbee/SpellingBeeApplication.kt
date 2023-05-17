package me.gibsoncodes.spellingbee

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

class SpellingBeeApplication :Application(){
    private val handlerThreadTag ="SpellingBeeHandlerThread"
    lateinit var handlerThread:HandlerThread
        private set
    lateinit var uiHandler:Handler
    private set
    override fun onCreate() {
        super.onCreate()
        uiHandler= Handler(Looper.getMainLooper())
        handlerThread= HandlerThread(handlerThreadTag).apply { start() }
    }
}