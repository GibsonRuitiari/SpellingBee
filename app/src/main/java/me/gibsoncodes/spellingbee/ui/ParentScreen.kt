package me.gibsoncodes.spellingbee.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
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
fun ParentScreen(puzzleRepository: PuzzleRepository,
                 puzzleGenerator: PuzzleGenerator,){

    val currentLifecycleOwner = LocalLifecycleOwner.current

    val events = remember { MutableSharedFlow<MainScreenEvents>(extraBufferCapacity = 10) }
    val puzzleDetailScreenEvents = remember { MutableSharedFlow<DetailsScreenEvents>(extraBufferCapacity = 10) }

    var currentScreen by rememberSaveable{ mutableStateOf<SpellingBeeScreens>(SpellingBeeScreens.MainPage) }
    val mainScreenState = mainScreenViewModel(events,puzzleGenerator, puzzleRepository)

    // a cheeky way of handling navigation, in essence this just switches the composables
    val navigateTo:(SpellingBeeScreens)->Unit ={screen->
        if (screen!=currentScreen){
            currentScreen=screen
        }
    }

    // it is difficult to know when a composable moves out of composition since there is no
    // on-stop method similar to that of an activity's but since a composable lives/is hosted inside an activity
    // we can track the lifecycle of the activity and refresh once the activity's on-stop method is called
    DisposableEffect(currentLifecycleOwner,currentLifecycleOwner.lifecycle){
        val observer = object :DefaultLifecycleObserver{

            override fun onResume(owner: LifecycleOwner) {
                if (currentScreen is SpellingBeeScreens.DetailsPage){
                   puzzleDetailScreenEvents.tryEmit(DetailsScreenEvents.LoadPuzzleEvent)
                }
            }
        }

        currentLifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            currentLifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            MainScreenView(mainScreenState, onEvent =events::tryEmit,
                onPuzzleClicked = {
                navigateTo(SpellingBeeScreens.DetailsPage(it))
            })
        }
    }

}
