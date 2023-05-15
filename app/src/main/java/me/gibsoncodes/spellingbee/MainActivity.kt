package me.gibsoncodes.spellingbee

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import me.gibsoncodes.spellingbee.di.AndroidModule
import me.gibsoncodes.spellingbee.di.FactoryManager
import me.gibsoncodes.spellingbee.di.InstancesCache
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.ParentScreen
import me.gibsoncodes.spellingbee.ui.theme.SpellingBeeTheme

@Suppress("DEPRECATION")
class MainActivity:androidx.activity.ComponentActivity() {
    private var androidComponent: AndroidModule?=null
    private var puzzleGenerator: PuzzleGenerator?=null

    private var isActivityDestroyedBySystem = false
    private val factoryManager by lazy { FactoryManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        androidComponent = AndroidModule(applicationContext)
        puzzleGenerator = PuzzleGeneratorDelegate(androidComponent!!.handlerThread.looper,androidComponent!!.assets)

        factoryManager.onActivityCreate(lastCustomNonConfigurationInstance as? InstancesCache)

        val db=androidComponent?.getDatabaseInstance()

        setContent {
            SpellingBeeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    // puzzleRepository = PuzzleRepositoryDelegate(lazyPuzzleDaoDelegate!!)
                    ParentScreen(factoryManager = factoryManager,dbInstance = db!!)
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
            false-> factoryManager.onActivityDestroy()
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
