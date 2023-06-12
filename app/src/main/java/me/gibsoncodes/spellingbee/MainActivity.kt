package me.gibsoncodes.spellingbee

import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import me.gibsoncodes.spellingbee.di.DefaultDependencyContainer
import me.gibsoncodes.spellingbee.log.warn
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper
import me.gibsoncodes.spellingbee.persistence.PuzzleDao
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.persistence.PuzzleRepositoryDelegate
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.ParentScreen
import me.gibsoncodes.spellingbee.ui.theme.SpellingBeeTheme
import me.gibsoncodes.spellingbee.utils.ifDebugDo

class MainActivity:androidx.activity.ComponentActivity() {
    private var isActivityDestroyedBySystem = false
    private lateinit var dependencyContainer: DefaultDependencyContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        val spellingBeeApplication = application as SpellingBeeApplication

        dependencyContainer = spellingBeeApplication.defaultDependencyContainer as DefaultDependencyContainer

        dependencyContainer.registerBinding(SQLiteOpenHelper::class,DatabaseHelper::class,applicationContext,BuildConfig.DatabaseName,BuildConfig.DatabaseVersion)
        dependencyContainer.registerBinding(PuzzleDao::class,PuzzleDaoDelegate::class, spellingBeeApplication.handlerThread)
        dependencyContainer.registerBinding(PuzzleRepository::class, PuzzleRepositoryDelegate::class)
        dependencyContainer.registerBinding(PuzzleGenerator::class, PuzzleGeneratorDelegate::class, spellingBeeApplication.handlerThread.looper, assets)

        val puzzleGenerator = dependencyContainer.resolveBinding<PuzzleGenerator>()
        val puzzleRepository= dependencyContainer.resolveBinding<PuzzleRepository>()

        setContent {
            SpellingBeeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
              ParentScreen(puzzleGenerator = puzzleGenerator,
                  puzzleRepository = puzzleRepository)
                }
            }
        }
    }
    override fun onResume() {
        isActivityDestroyedBySystem=false
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        isActivityDestroyedBySystem=true
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        when(isActivityDestroyedBySystem){
            false->{
                ifDebugDo { warn<MainActivity> { "Activity is being destroyed but not by the system rather by the user! Disposing our bindings." } }
                dependencyContainer.dispose()
            }
            else->{
                ifDebugDo { warn<MainActivity> {"Activity is being destroyed by the system. Dependency bindings will be automatically destroyed."} }
                // do not remove we will use the cached instance to restore our dependency
            }
        }
        super.onDestroy()
    }

}
