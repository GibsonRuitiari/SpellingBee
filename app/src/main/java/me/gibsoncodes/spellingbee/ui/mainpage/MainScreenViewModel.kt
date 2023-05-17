package me.gibsoncodes.spellingbee.ui.mainpage

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState.Companion.blankGameState
import me.gibsoncodes.spellingbee.ui.PuzzleUi.Companion.toPuzzleUi


@Composable
fun mainScreenViewModel(uiEvents:Flow<MainScreenEvents>,
                        puzzleGenerator: PuzzleGenerator,
                        puzzleRepository: PuzzleRepository):MainScreenState{
    // survive state through configuration changes and process kill
    var uiState by rememberSaveable { mutableStateOf<MainScreenState>(MainScreenState.MainScreenLoading) }
    var uiEvent by remember { mutableStateOf<MainScreenEvents>(MainScreenEvents.LoadCachedPuzzleBoardStates) }

    val puzzleBoardStates = remember { mutableSetOf<PuzzleBoardState>() }

    LaunchedEffect(uiEvents,Unit){
        uiEvents.collect{event->
            uiEvent= event
        }
    }

    LaunchedEffect(uiEvent){
            uiState = MainScreenState.MainScreenLoading
            when(uiEvent){
                is MainScreenEvents.LoadCachedPuzzleBoardStates->{
                    uiState = MainScreenState.MainScreenLoading
                    puzzleBoardStates.clear()
                    val _puzzleBoardStates=puzzleRepository.getCachedPuzzleBoardStates()
                    puzzleBoardStates.addAll(_puzzleBoardStates)
                    delay(1000)
                    uiState = MainScreenState.MainScreenData(puzzleBoardStates = puzzleBoardStates)
                }

                is MainScreenEvents.OnDeletePuzzle->{
                    val puzzleId = (uiEvent as MainScreenEvents.OnDeletePuzzle).puzzleId
                    puzzleRepository.deleteCachedPuzzleById(puzzleId)
                    uiEvent = MainScreenEvents.RefreshPuzzleBoardStates
                }

                is MainScreenEvents.RefreshPuzzleBoardStates->{
                    puzzleRepository.refreshCachedPuzzleBoardStates()
                    uiEvent = MainScreenEvents.LoadCachedPuzzleBoardStates
                }

                is MainScreenEvents.OnInsertPuzzle->{
                    uiState = MainScreenState.MainScreenLoading

                    val puzzleUi =(uiEvent as MainScreenEvents.OnInsertPuzzle).puzzleUi
                    val puzzleGameState = puzzleUi.blankGameState()

                    puzzleRepository.insertOrIgnorePuzzle(puzzleUi)
                    puzzleRepository.insertOrReplacePuzzleGameState(puzzleGameState,puzzleUi.id)
                    uiEvent = MainScreenEvents.RefreshPuzzleBoardStates
                }

                is MainScreenEvents.GeneratePuzzle->{
                    uiState = MainScreenState.MainScreenLoading
                    val puzzle=puzzleGenerator.generatePuzzle(0)
                    if (puzzle==null){
                        uiState = MainScreenState.MainScreenError("an error occurred while generating the puzzle please try again later")
                    }else{
                        uiEvent = MainScreenEvents.OnInsertPuzzle(puzzle.toPuzzleUi())
                    }
                }
            }
    }

    return  uiState
}