package me.gibsoncodes.spellingbee

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.HandlerThread
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import me.gibsoncodes.spellingbee.di.*
import me.gibsoncodes.spellingbee.persistence.PuzzleDao
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.persistence.PuzzleRepositoryDelegate
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.ParentScreen
import me.gibsoncodes.spellingbee.ui.theme.SpellingBeeTheme
import me.gibsoncodes.spellingbee.utils.ifDebugDo

@Suppress("DEPRECATION")
class MainActivity:androidx.activity.ComponentActivity() {
    private var androidComponent: AndroidModule?=null
    private var puzzleGenerator: PuzzleGenerator?=null

    private var isActivityDestroyedBySystem = false
    private val factoryManager by lazy { FactoryManager }
    private val defaultCreationExtras by lazy { MutableCreationExtras() }


    companion object{
        private object HandlerThreadKeyImpl: CreationExtras.Key<HandlerThread>
        private object DatabaseKeyImpl: CreationExtras.Key<SQLiteDatabase>
        private object PuzzleDaoKeyImpl:CreationExtras.Key<PuzzleDao>
        private val PuzzleDaoKey:CreationExtras.Key<PuzzleDao> =PuzzleDaoKeyImpl
        private val DatabaseKey: CreationExtras.Key<SQLiteDatabase> = DatabaseKeyImpl
        private val HandlerThreadKey: CreationExtras.Key<HandlerThread> =HandlerThreadKeyImpl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        androidComponent = AndroidModule(applicationContext)
        puzzleGenerator = PuzzleGeneratorDelegate(androidComponent!!)

        factoryManager.onActivityCreate(lastCustomNonConfigurationInstance as? InstancesCache)

        val secondPuzzleDao by factoryManager.create<PuzzleDao>(1,
            creationExtras=defaultCreationExtras.apply {
            this[DatabaseKey] = androidComponent!!.getDatabaseInstance(inMemory = false)!!
            this[HandlerThreadKey] = androidComponent!!.handlerThread
        }, modelClass = PuzzleDao::class.java
            ,createObject = {key, _,creationExtras ->
                return@create when (key) {
                    1 -> {
                        val handlerThread = creationExtras[HandlerThreadKey]
                        val databaseInstance = creationExtras[DatabaseKey]
                        requireNotNull(handlerThread) { throw IllegalArgumentException("Must provide a handler thread!") }
                        requireNotNull(databaseInstance) { throw IllegalArgumentException("Must provide a database instance!!") }
                        PuzzleDaoDelegate(databaseInstance,handlerThread)
                    }
                    else -> throw IllegalArgumentException("Key provided is not recognized!!")
                }
            })


        val puzzleRepository by factoryManager.create(2, creationExtras = defaultCreationExtras.apply {
            this[PuzzleDaoKey] =secondPuzzleDao!!
        }, modelClass = PuzzleRepository::class.java, createObject = {key, modelClass, creationExtras ->
            when(key){
                2->{
                    val puzzleDao =creationExtras[PuzzleDaoKey]
                    requireNotNull(puzzleDao)
                    PuzzleRepositoryDelegate(puzzleDao)
                }
                else->throw IllegalArgumentException("key provided is not recognizable!")
            }
        })


        ifDebugDo {println("is puzzle repository null? ${puzzleRepository==null} hash-code of  puzzle repo in ${puzzleRepository?.hashCode()}") }

        ifDebugDo {println("is second puzzle dao null? ${secondPuzzleDao==null} hash-code of  second puzzle dao delegate in ${secondPuzzleDao?.hashCode()}") }

        setContent {
            SpellingBeeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    // puzzleRepository = PuzzleRepositoryDelegate(lazyPuzzleDaoDelegate!!)
                   // ParentScreen(puzzleGenerator = puzzleGenerator!!, puzzleRepository = puzzleRepository!!)
                    Text("Hello there!!")
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
