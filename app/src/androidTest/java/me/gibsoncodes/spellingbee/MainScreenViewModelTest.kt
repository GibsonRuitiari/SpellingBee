package me.gibsoncodes.spellingbee

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenEvents
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenState
import me.gibsoncodes.spellingbee.ui.mainpage.MainScreenView
import me.gibsoncodes.spellingbee.ui.mainpage.mainScreenViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenViewModelTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    private lateinit var puzzleRepository: PuzzleRepository
    private lateinit var puzzleGenerator:PuzzleGenerator
    @Before
    fun setUp(){
        puzzleRepository = FakePuzzleRepository()
        puzzleGenerator = FakePuzzleGenerator
    }
    @Test
    fun initialTest(){
        val uiEvents = MutableSharedFlow<MainScreenEvents>(extraBufferCapacity = 10)
        var uiState by mutableStateOf<MainScreenState>(MainScreenState.MainScreenLoading)


        composeTestRule.activity.setContent {
            uiState= mainScreenViewModel(puzzleRepository = puzzleRepository,
                puzzleGenerator = puzzleGenerator, uiEvents = uiEvents)
            MainScreenView(mainScreenState = uiState,
                onEvent = {
                uiEvents.tryEmit(it)
            },onPuzzleClicked = {})
        }
    }
}