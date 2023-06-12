package me.gibsoncodes.spellingbee

import android.app.Application
import android.database.sqlite.SQLiteOpenHelper
import android.os.HandlerThread
import me.gibsoncodes.spellingbee.di.DefaultDependencyContainer
import me.gibsoncodes.spellingbee.di.DependencyContainer
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper
import me.gibsoncodes.spellingbee.persistence.PuzzleDao
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.persistence.PuzzleRepositoryDelegate
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate

class SpellingBeeApplication :Application(){
    private val handlerThreadTag ="SpellingBeeHandlerThread"
    lateinit var handlerThread:HandlerThread
        private set
    lateinit var defaultDependencyContainer: DependencyContainer


    override fun onCreate() {
        super.onCreate()
        handlerThread= HandlerThread(handlerThreadTag).apply { start() }
        defaultDependencyContainer = DefaultDependencyContainer.getInstance()

        defaultDependencyContainer.registerBinding(
            SQLiteOpenHelper::class,
            DatabaseHelper::class,applicationContext,BuildConfig.DatabaseName,BuildConfig.DatabaseVersion)
        defaultDependencyContainer.registerBinding(
            PuzzleDao::class,
            PuzzleDaoDelegate::class, handlerThread)
        defaultDependencyContainer.registerBinding(PuzzleRepository::class, PuzzleRepositoryDelegate::class)
        defaultDependencyContainer.registerBinding(PuzzleGenerator::class, PuzzleGeneratorDelegate::class,handlerThread.looper, assets)

    }
}