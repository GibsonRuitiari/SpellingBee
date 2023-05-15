@file:Suppress("DEPRECATION")

package me.gibsoncodes.spellingbee.ui

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.HandlerThread
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import me.gibsoncodes.spellingbee.di.FactoryManager
import me.gibsoncodes.spellingbee.di.InstancesCache
import me.gibsoncodes.spellingbee.di.create
import me.gibsoncodes.spellingbee.persistence.PuzzleDao
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.persistence.PuzzleRepositoryDelegate
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import me.gibsoncodes.spellingbee.ui.detailspage.DetailsScreenEvents
import me.gibsoncodes.spellingbee.ui.detailspage.DetailsScreenUiContent
import me.gibsoncodes.spellingbee.ui.detailspage.detailsScreenViewModel
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenEvents
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenView
import me.gibsoncodes.spellingbee.ui.mainpage.mainScreenViewModel
import me.gibsoncodes.spellingbee.ui.navigation.SpellingBeeScreens
import me.gibsoncodes.spellingbee.utils.ifDebugDo

private const val HandlerThreadName="HandlerThreadTag"
@Composable
fun ParentScreen(factoryManager:FactoryManager,
                 dbInstance:SQLiteDatabase){

    val localContext = LocalContext.current

    val localDefaultCreationExtras by remember { mutableStateOf(MutableCreationExtras()) }
    var localPuzzleGenerator by remember { mutableStateOf<PuzzleGenerator?>(null) }
    val localHandlerThread = remember { HandlerThread(HandlerThreadName).apply { start() } }
    val localLastCustomNonConfigurationInstance = remember { (localContext as? ComponentActivity)?.lastCustomNonConfigurationInstance }
    var localPuzzleDao by remember { mutableStateOf<PuzzleDao?>(null) }
    var localPuzzleRepository by remember { mutableStateOf<PuzzleRepository?>(null) }



    val constructPuzzleGenerator:()->PuzzleGenerator={
        PuzzleGeneratorDelegate(looper=localHandlerThread.looper,assets = localContext.assets)
    }



    val constructPuzzleDao:()->PuzzleDao? = {
        val dao by factoryManager.create(CreationExtrasKeys.PuzzleDaoFactoryKey,
            creationExtras =localDefaultCreationExtras.apply {
                this[CreationExtrasKeys.DatabaseKey] = dbInstance
                this[CreationExtrasKeys.HandlerThreadKey] = localHandlerThread
            },modelClass = PuzzleDao::class.java,
            createObject = {key, _, creationExtras ->
                return@create when (key) {
                    1 -> {
                        val handlerThread = creationExtras[CreationExtrasKeys.HandlerThreadKey]
                        val databaseInstance = creationExtras[CreationExtrasKeys.DatabaseKey]
                        requireNotNull(handlerThread) { throw IllegalArgumentException("Must provide a handler thread!") }
                        requireNotNull(databaseInstance) { throw IllegalArgumentException("Must provide a database instance!!") }
                        PuzzleDaoDelegate(databaseInstance,handlerThread)
                    }
                    else -> throw IllegalArgumentException("Key provided is not recognized!!")
                }
            })
        dao
    }

    val constructPuzzleRepository:(puzzleDao:PuzzleDao)->PuzzleRepository? = {
        val repository by factoryManager.create(CreationExtrasKeys.PuzzleRepositoryFactoryKey, creationExtras = localDefaultCreationExtras.apply {
            this[CreationExtrasKeys.PuzzleDaoKey] =it
        }, modelClass = PuzzleRepository::class.java, createObject = {key, _, creationExtras ->
            when(key){
                2->{
                    val puzzleDao =creationExtras[CreationExtrasKeys.PuzzleDaoKey]
                    requireNotNull(puzzleDao)
                    PuzzleRepositoryDelegate(puzzleDao)
                }
                else->throw IllegalArgumentException("key provided is not recognizable!")
            }
        })
        repository
    }

    val cleanUpDependencies = {
        localPuzzleGenerator?.let { localPuzzleGenerator = null }
        localPuzzleDao?.let { localPuzzleDao=null }
        localPuzzleRepository?.let { localPuzzleRepository=null }

    }



    val events = remember { MutableSharedFlow<MainScreenEvents>(extraBufferCapacity = 10) }
    val puzzleDetailScreenEvents = remember { MutableSharedFlow<DetailsScreenEvents>(extraBufferCapacity = 10) }

    var currentScreen by rememberSaveable{ mutableStateOf<SpellingBeeScreens>(SpellingBeeScreens.MainPage) }


    // a cheeky way of handling navigation, in essence this just switches the composables
    val navigateTo:(SpellingBeeScreens)->Unit ={screen->
        if (screen!=currentScreen){
            currentScreen=screen
        }
    }

    LifecycleEventEffect(lifecycleEvent = Lifecycle.Event.ON_RESUME, runOnEvent = {
        if (currentScreen is SpellingBeeScreens.DetailsPage){
            puzzleDetailScreenEvents.tryEmit(DetailsScreenEvents.LoadPuzzleEvent)
        }
    })

    LifecycleEventStartAndOnStop(runOnCreate = {
        localPuzzleGenerator = constructPuzzleGenerator()
        factoryManager.onActivityCreate(localLastCustomNonConfigurationInstance as? InstancesCache)
        localPuzzleDao=constructPuzzleDao()
        localPuzzleRepository=constructPuzzleRepository(localPuzzleDao!!)

    },runOnStop = {
       cleanUpDependencies()
    })

    LaunchedEffect(localPuzzleDao,localPuzzleRepository){
        ifDebugDo { println("puzzle repository hash code ${localPuzzleRepository?.hashCode()}") }
        ifDebugDo { println("puzzle dao hash code ${localPuzzleDao?.hashCode()}") }
    }

    val activity = LocalContext.current as? Activity

    BackHandler {
        if (currentScreen!=SpellingBeeScreens.MainPage){
            /* A little trick to ensure the composable gets composed/recomposed with fresh data always */
            events.tryEmit(MainScreenEvents.RefreshPuzzleBoardStates)
            currentScreen=SpellingBeeScreens.MainPage
        }else activity?.finish()
    }

    when(currentScreen){
        is SpellingBeeScreens.DetailsPage->{
            val puzzleDetailsScreenState = detailsScreenViewModel((currentScreen as SpellingBeeScreens.DetailsPage).puzzleId,
                uiEvents = puzzleDetailScreenEvents,
                puzzleRepository = localPuzzleRepository!!)

            DetailsScreenUiContent(detailsScreenState =puzzleDetailsScreenState,
                navigateTo = {
                    events.tryEmit(MainScreenEvents.RefreshPuzzleBoardStates)
                    navigateTo(it)
                 },
                onEvent = {
                    puzzleDetailScreenEvents.tryEmit(it)
                })

        }
        SpellingBeeScreens.MainPage->{
            val mainScreenState = mainScreenViewModel(events, puzzleGenerator = localPuzzleGenerator!!, localPuzzleRepository!!)
            MainScreenView(mainScreenState, onEvent =events::tryEmit,
                onPuzzleClicked = {
                navigateTo(SpellingBeeScreens.DetailsPage(it))
            })
        }
    }
}

@Composable
private fun LifecycleEventEffect(lifecycleOwner:LifecycleOwner=LocalLifecycleOwner.current,
                                 lifecycleEvent:Lifecycle.Event,
                                 runOnEvent:()->Unit){
    require(lifecycleEvent!=Lifecycle.Event.ON_DESTROY){"Cannot observe ON_DESTROY because Compose disposes composition before ON_DESTROY is called"}
    val currentEffect by rememberUpdatedState(runOnEvent)
    DisposableEffect(lifecycleOwner){
        val observer = LifecycleEventObserver{_,event ->
            if (event==lifecycleEvent){
                currentEffect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun LifecycleEventStartAndOnStop(lifecycleOwner:LifecycleOwner=LocalLifecycleOwner.current,
                                         runOnCreate:()->Unit,runOnStop:()->Unit){
    val currentEffect by rememberUpdatedState(runOnCreate)
    DisposableEffect(lifecycleOwner){
        val observer = LifecycleEventObserver{_,event ->
            when(event){
                Lifecycle.Event.ON_CREATE->currentEffect()
                Lifecycle.Event.ON_STOP->runOnStop()
                else->{}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
internal object CreationExtrasKeys {
    private object ContextKeyImpl:CreationExtras.Key<Context>
    private object DatabaseNameKeyImpl:CreationExtras.Key<String>
    private object DatabaseVersionKeyImpl:CreationExtras.Key<Int>
    private object HandlerThreadKeyImpl: CreationExtras.Key<HandlerThread>
    private object DatabaseKeyImpl: CreationExtras.Key<SQLiteDatabase>
    private object PuzzleDaoKeyImpl: CreationExtras.Key<PuzzleDao>
    val PuzzleDaoKey: CreationExtras.Key<PuzzleDao> =PuzzleDaoKeyImpl
    val DatabaseKey: CreationExtras.Key<SQLiteDatabase> = DatabaseKeyImpl
    val HandlerThreadKey: CreationExtras.Key<HandlerThread> =HandlerThreadKeyImpl
    val DatabaseVersionKey:CreationExtras.Key<Int> = DatabaseVersionKeyImpl
    val ContextKey:CreationExtras.Key<Context> = ContextKeyImpl
    val DatabaseNameKey:CreationExtras.Key<String> = DatabaseNameKeyImpl

    const val PuzzleDaoFactoryKey =0x001
    const val PuzzleRepositoryFactoryKey =0x002

}