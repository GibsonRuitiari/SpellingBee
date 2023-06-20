package me.gibsoncodes.spellingbee


import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenEvents
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenState
import me.gibsoncodes.spellingbee.ui.mainpage.mainScreenViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertTrue


class MainScreenViewModelTest {
    /**
     * A little explanation as to the testing
     * Android framework uses a clock to broadcast/send events between components
     * The Ui is refreshed after a certain passage of time called frame
     * Testing therefore of the ui has to be done within the Android framework clock for it be correct
     * in otherwords, you must mimic the clock for this test to pass and that involves telling compose when
     * to compose and recompose
     * Hence why we are using moleculeFlow's RecompositionClock
     * The Recomposition clock schedules the recomposition and composition
     * So for every event, a frame is sent/pushed to the clock and compose does it works (either recomposes or composes *for the initial frame*)
     */
    @Test
    fun stateFromMainScreenViewModel_is_Correct_Given_LoadCachedPuzzlesEvent() = runBlocking {
        val events = MutableSharedFlow<MainScreenEvents>()
        val repository = FakePuzzleRepository()
        val puzzleGenerator = FakePuzzleGenerator
        val cachedPuzzles = repository.getCachedPuzzleBoardStates()
        moleculeFlow(RecompositionClock.Immediate){
            mainScreenViewModel(uiEvents = events,
                puzzleGenerator =puzzleGenerator,
                puzzleRepository = repository)
        }.test {
            // fire up any launched effect's inside the main-screen-view-model
            yield()
            val beginningState = awaitItem()
            assertEquals(MainScreenState.MainScreenLoading,beginningState)
            events.emit(MainScreenEvents.LoadCachedPuzzleBoardStates)
            val finalState = awaitItem()
            // no intermediate step here
            assertTrue { finalState is MainScreenState.MainScreenData }
            val mainScreenData = finalState as MainScreenState.MainScreenData
            assertTrue { mainScreenData.puzzleBoardStates.size == cachedPuzzles.size }
            assert(mainScreenData.puzzleBoardStates.containsAll(cachedPuzzles))
            events.emit(MainScreenEvents.RefreshPuzzleBoardStates)
            val refreshedState = awaitItem()
            assertTrue { refreshedState is MainScreenState.MainScreenData }
        }
    }
}