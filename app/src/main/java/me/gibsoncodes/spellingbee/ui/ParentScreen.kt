package me.gibsoncodes.spellingbee.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.ui.detailspage.DetailsScreenEvents
import me.gibsoncodes.spellingbee.ui.detailspage.DetailsScreenUiContent
import me.gibsoncodes.spellingbee.ui.detailspage.detailsScreenViewModel
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenEvents
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenView
import me.gibsoncodes.spellingbee.ui.mainpage.mainScreenViewModel
import me.gibsoncodes.spellingbee.ui.navigation.SpellingBeeScreens

@Composable
fun ParentScreen(puzzleGenerator: PuzzleGenerator,
                 puzzleRepository: PuzzleRepository){
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
                puzzleRepository = puzzleRepository)

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
            val mainScreenState = mainScreenViewModel(events, puzzleGenerator = puzzleGenerator,puzzleRepository)
            val configuration = LocalConfiguration.current
            var initialConfiguration by remember{ mutableStateOf(Configuration.ORIENTATION_PORTRAIT) }

            LaunchedEffect(configuration){
                events.tryEmit(MainScreenEvents.RefreshPuzzleBoardStates)
                initialConfiguration = configuration.orientation
            }

            MainScreenView(mainScreenState, onEvent =events::tryEmit,
                onPuzzleClicked = { navigateTo(SpellingBeeScreens.DetailsPage(it)) })
        }
    }

}

@Composable
private fun LifecycleEventEffect(lifecycleOwner:LifecycleOwner=LocalLifecycleOwner.current,
                                 lifecycleEvent:Lifecycle.Event,
                                 runOnEvent:()->Unit){
    require(lifecycleEvent!=Lifecycle.Event.ON_DESTROY){"Cannot observe ON_DESTROY because Compose disposes composition before ON_DESTROY is called"}
    val currentEffect by rememberUpdatedState(runOnEvent)
    DisposableEffect(lifecycleOwner,lifecycleEvent,Unit){
        val observer = LifecycleEventObserver{_,event ->
            if (event==lifecycleEvent){
                currentEffect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

