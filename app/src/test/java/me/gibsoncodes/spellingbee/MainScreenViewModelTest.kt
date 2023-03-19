package me.gibsoncodes.spellingbee

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenEvents
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenState
import me.gibsoncodes.spellingbee.ui.mainpage.mainScreenViewModel
import org.junit.Assert.assertEquals
import org.junit.Test


class MainScreenViewModelTest {
    // throws a coroutine not completing error
    // need to figure out how to actually test the presenters/view-model
    @Test
    fun testMainScreenViewModel() = runTest {
        val events = MutableSharedFlow<MainScreenEvents>()
        val repository = FakePuzzleRepository()
        val puzzleGenerator =FakePuzzleGenerator
        val cachedPuzzles = repository.getCachedPuzzleBoardStates()
        moleculeFlow(clock = RecompositionClock.Immediate){
             mainScreenViewModel(events,puzzleGenerator,repository)
        }.test {
            yield() // fire up initial launched-effect
            assertEquals(MainScreenState.MainScreenLoading,
                awaitItem())

            if (awaitItem() is MainScreenState.MainScreenData){
               val loadedPuzzleBoardStates= (awaitItem() as MainScreenState.MainScreenData).puzzleBoardStates
                assertEquals(loadedPuzzleBoardStates, cachedPuzzles)
            }

            events.emit(MainScreenEvents.RefreshPuzzleBoardStates)
            assertEquals(MainScreenState.MainScreenLoading, awaitItem())

        }
    }
}