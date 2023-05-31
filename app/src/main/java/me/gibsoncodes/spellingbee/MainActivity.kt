package me.gibsoncodes.spellingbee

import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import me.gibsoncodes.spellingbee.di.DependenciesContainer
import me.gibsoncodes.spellingbee.di.DependenciesContainer.defaultOpenHelper
import me.gibsoncodes.spellingbee.di.DependenciesContainer.getPuzzleDao
import me.gibsoncodes.spellingbee.di.DependenciesContainer.puzzleGenerator
import me.gibsoncodes.spellingbee.di.DependenciesContainer.puzzleRepository
import me.gibsoncodes.spellingbee.di.InstancesCache
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.ParentScreen
import me.gibsoncodes.spellingbee.ui.theme.SpellingBeeTheme
import me.gibsoncodes.spellingbee.utils.getDatabaseInstance

@Suppress("DEPRECATION")
class MainActivity:androidx.activity.ComponentActivity() {
    private var isActivityDestroyedBySystem = false
    private val factoryManager by lazy { DependenciesContainer.factoryManager }
    private lateinit var sqliteOpenHelper:SQLiteOpenHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val applicationHandlerThread =(application as SpellingBeeApplication).handlerThread

        factoryManager.onActivityCreate(lastCustomNonConfigurationInstance as? InstancesCache)
        sqliteOpenHelper=applicationContext.defaultOpenHelper
        val database=sqliteOpenHelper.getDatabaseInstance(applicationHandlerThread.looper)

        setContent {
            SpellingBeeTheme {

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    ParentScreen(puzzleGenerator = puzzleGenerator as PuzzleGeneratorDelegate,
                        puzzleRepository = getPuzzleDao(database!!).puzzleRepository)

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
            false-> {
                factoryManager.onActivityDestroy()
                sqliteOpenHelper.close()
            }
            else-> {
                // do not remove we will use the cached instance to restore our dependency
            }
        }
        super.onDestroy()
    }

    override fun onRetainCustomNonConfigurationInstance(): InstancesCache {
         return factoryManager.instanceCache
    }

}
