package me.gibsoncodes.spellingbee

import android.app.Application
import android.os.HandlerThread
import me.gibsoncodes.spellingbee.di.DependencyContainer
import me.gibsoncodes.spellingbee.di.startContainer

class SpellingBeeApplication :Application(){
    private val handlerThreadTag ="SpellingBeeHandlerThread"
    lateinit var handlerThread:HandlerThread
        private set
    lateinit var defaultDependencyContainer: DependencyContainer


    override fun onCreate() {
        super.onCreate()
        handlerThread= HandlerThread(handlerThreadTag).apply { start() }
        defaultDependencyContainer = startContainer()
    }
}