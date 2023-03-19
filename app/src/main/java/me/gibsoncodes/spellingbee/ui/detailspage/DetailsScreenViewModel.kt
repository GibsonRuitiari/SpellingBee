package me.gibsoncodes.spellingbee.ui.detailspage

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.Flow
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState.Companion.blankGameState
import me.gibsoncodes.spellingbee.ui.PuzzleUi
import me.gibsoncodes.spellingbee.ui.WordResult
import me.gibsoncodes.spellingbee.ui.detailspage.DetailsScreenState.DetailsScreenData.Companion.toDetailsScreenData
import me.gibsoncodes.spellingbee.utils.ifDebugDo
import java.util.*


@Composable
fun detailsScreenViewModel(puzzleId:Long,
                           uiEvents:Flow<DetailsScreenEvents>,
                           puzzleRepository:PuzzleRepository):DetailsScreenState{

    var uiState by remember{ mutableStateOf<DetailsScreenState>(DetailsScreenState.DetailsScreenLoading) }
    var puzzleBoardState by rememberSaveable{ mutableStateOf<PuzzleBoardState?>(null) }
    var screenEvent by remember { mutableStateOf<DetailsScreenEvents>(DetailsScreenEvents.LoadPuzzleEvent) }

    var activeWordToast by remember { mutableStateOf<WordToast?>(null) }
    var activeDialog by rememberSaveable{ mutableStateOf<PuzzleDetailsDialog?>(null) }


    LaunchedEffect(uiEvents){
        uiEvents.collect{uiEvent->
            screenEvent= uiEvent
        }
    }


    puzzleBoardState?.let {existingBoardState->
        LaunchedEffect(existingBoardState.gameState){
            ifDebugDo { println(" ---- saving game state---") }
            puzzleRepository.updateGameState(existingBoardState.gameState,existingBoardState.puzzle.id)
        }
    }

    LaunchedEffect(screenEvent){
        uiState = DetailsScreenState.DetailsScreenLoading
        when(screenEvent){

            is DetailsScreenEvents.DeleteKeypadEvent -> {
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                with(currentBoardState){
                    val currentGameState = gameState
                    val newWord = currentGameState.currentWord.dropLast(1)
                    val newBoardState = currentGameState.copy(currentWord = newWord)

                    puzzleBoardState = PuzzleBoardState(puzzle,newBoardState)

                    uiState = puzzleBoardState!!.toDetailsScreenData(activeWordToast, activeDialog)

                }

            }

            DetailsScreenEvents.ShowAnswersDialogEvent->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                with(currentBoardState){
                    val answers = puzzle.answers.map { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
                        }
                    }.sorted()
                    activeDialog = PuzzleDetailsDialog.AnswersDialog(answers=answers,
                        found =gameState.discoveredWords,
                        pangrams = puzzle.pangrams)
                    uiState = toDetailsScreenData(activeWordToast, activeDialog)
                }

            }
            DetailsScreenEvents.DismissActiveToastEvent -> {
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                activeWordToast=null
                uiState = currentBoardState.toDetailsScreenData(null, activeDialog)
            }

            is DetailsScreenEvents.EnterKeypadEvent ->{
                val oldPuzzleBoardState = puzzleBoardState ?: return@LaunchedEffect
                with(oldPuzzleBoardState){
                    val currentGameState = gameState
                    val enteredWord = currentGameState.currentWord
                    if (enteredWord.isBlank()) return@with
                    when(val wordResult = checkWordIfValid(enteredWord)){
                        is WordResult.BadWord ->{
                            val newBoardState=copy(gameState = currentGameState.copy(currentWord = ""))
                            puzzleBoardState = newBoardState

                            activeWordToast = WordToast.Error(wordResult.errorType)

                            uiState=puzzleBoardState!!.toDetailsScreenData(activeWordToast)
                        }

                        WordResult.ValidWord ->{
                            val discoveredSet=currentGameState.discoveredWords.plus(enteredWord)
                            val newBoardState=copy(gameState =currentGameState.copy(currentWord = "",
                                discoveredWords = discoveredSet))

                            puzzleBoardState = newBoardState

                            activeWordToast =WordToast.Success(puzzle.getScoreOfWord(enteredWord))

                            uiState=puzzleBoardState!!.toDetailsScreenData(activeWordToast)
                        }
                    }
                }
            }
            DetailsScreenEvents.ShowInfoDialogEvent->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                activeDialog = PuzzleDetailsDialog.InfoDialog
                uiState = currentBoardState.toDetailsScreenData(activeWordToast, activeDialog)

            }

            is DetailsScreenEvents.KeyPressEvent ->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect

                with(currentBoardState){
                    val currentGameState = gameState
                    val currentWord = currentGameState.currentWord

                    ifDebugDo { println("current word----> $currentWord") }

                    if (currentWord.length >= PuzzleUi.MAXIMUM_WORD_LENGTH){
                        screenEvent=DetailsScreenEvents.EnterKeypadEvent(gameState.currentWord)
                        return@with
                    }
                    val pressedWord = (screenEvent as DetailsScreenEvents.KeyPressEvent).pressedChar

                    val newWord = currentWord.plus(pressedWord)
                    val newGameState=gameState.copy(currentWord =newWord)

                    puzzleBoardState=PuzzleBoardState(currentBoardState.puzzle,newGameState)

                    uiState=puzzleBoardState!!.toDetailsScreenData(activeWordToast, activeDialog)

                    screenEvent=DetailsScreenEvents.SentinielEvent
                }

            }

            DetailsScreenEvents.LoadPuzzleEvent ->{
                val puzzleUi=puzzleRepository.getPuzzleById(puzzleId) ?: return@LaunchedEffect
                val puzzleGameState = puzzleRepository.getGameStateById(puzzleId) ?: puzzleUi.blankGameState()
                puzzleBoardState=PuzzleBoardState(puzzle = puzzleUi,puzzleGameState)
                uiState=puzzleBoardState!!.toDetailsScreenData(activeWordToast)
            }

            DetailsScreenEvents.ResetGameEvent ->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                with(currentBoardState){
                    val newGameState=puzzle.blankGameState()
                    val newPuzzleBoardState = PuzzleBoardState(puzzle,newGameState)
                    puzzleBoardState = newPuzzleBoardState
                    uiState = puzzleBoardState!!.toDetailsScreenData(null)
                }
            }

            is DetailsScreenEvents.ShuffleKeypadEvent ->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                with(currentBoardState){
                    val newGameState = gameState.copy(outerLetters = gameState.outerLetters.shuffled())
                    puzzleBoardState = PuzzleBoardState(puzzle, newGameState)
                    uiState = puzzleBoardState!!.toDetailsScreenData(activeWordToast, activeDialog)
                }
            }

            DetailsScreenEvents.ShowConfirmResetDialogEvent ->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                activeDialog = PuzzleDetailsDialog.ConfirmResetDialog
                uiState=currentBoardState.toDetailsScreenData(activeWordToast, activeDialog)
            }

            DetailsScreenEvents.ShowRankingDialogEvent->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                val maximumScore=currentBoardState.puzzle.maximumPuzzleScore
                activeDialog = PuzzleDetailsDialog.RankingDialog(maximumScore)
                uiState = currentBoardState.toDetailsScreenData(activeWordToast, activeDialog)

            }
            DetailsScreenEvents.DismissActiveDialogEvent->{
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                activeDialog = null
                uiState=currentBoardState.toDetailsScreenData(activeWordToast,null)
            }

            // shared-flow as with all other flows does not take the same event/value twice
            // so this sentiniel event acts as a fake event that does nothing other than just re-assign the
            // uiState to the existing puzzle game state, this way we can take multiple repetitive events successfully
            // this is only applicable in the Keypress event whereby the character maybe pressed twice
            // consider sassy where the third and fourth 's' are repeated,
            // without this fake event only the third 's' will be delivered to our consumer
            // the fourth would be lost hence the importance of this event
            // a channel might work in this case but please note in case of multiple subscribers only one of them will receive the
            // the ui events

            DetailsScreenEvents.SentinielEvent->{
                //do nothing here
                val currentBoardState = puzzleBoardState ?: return@LaunchedEffect
                uiState=currentBoardState.toDetailsScreenData(activeWordToast,activeDialog)
            }

            DetailsScreenEvents.PersistGameState->{
                puzzleBoardState?.let {
                    puzzleRepository.updateGameState(it.gameState,it.puzzle.id)
                }
            }

        }
    }


    return uiState
}