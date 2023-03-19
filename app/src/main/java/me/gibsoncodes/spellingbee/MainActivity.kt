package me.gibsoncodes.spellingbee

import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import me.gibsoncodes.spellingbee.di.AndroidModule
import me.gibsoncodes.spellingbee.di.AndroidModule.Companion.DatabaseName
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.persistence.PuzzleRepositoryDelegate
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.ParentScreen
import me.gibsoncodes.spellingbee.ui.theme.SpellingBeeTheme

class MainActivity:ComponentActivity() {
    private var databaseHelper: SQLiteOpenHelper?=null
    private var androidComponent: AndroidModule?=null
    private var puzzleRepository: PuzzleRepository?=null
    private var puzzleGenerator: PuzzleGenerator?=null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            androidComponent = AndroidModule(applicationContext)
            databaseHelper= androidComponent!!.getDatabaseHelper()

            val database =androidComponent!!.getDatabaseInstance()

            puzzleGenerator = PuzzleGeneratorDelegate(androidComponent!!)

            val puzzleDaoDelegate = PuzzleDaoDelegate(database!!,androidComponent!!.handlerThread)
            puzzleRepository = PuzzleRepositoryDelegate(puzzleDaoDelegate)
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            databaseHelper?.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(lifecycleObserver)

        setContent {
            SpellingBeeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    ParentScreen(puzzleGenerator = puzzleGenerator!!,
                        puzzleRepository = puzzleRepository!!
                    )
                }
            }
        }
    }
}
