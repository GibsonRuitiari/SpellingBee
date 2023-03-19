package me.gibsoncodes.spellingbee.ui.detailspage

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleRanking
import me.gibsoncodes.spellingbee.ui.WordError

sealed interface DetailsScreenEvents{
    @JvmInline
    value class EnterKeypadEvent(val currentWord: String):DetailsScreenEvents
    @JvmInline
    value class DeleteKeypadEvent(val currentWord:String):DetailsScreenEvents
    @JvmInline
    value class ShuffleKeypadEvent(val outerLetters:List<Char>):DetailsScreenEvents
    @JvmInline
    value class KeyPressEvent(val pressedChar:Char):DetailsScreenEvents

    object DismissActiveToastEvent:DetailsScreenEvents

    object ShowConfirmResetDialogEvent:DetailsScreenEvents
    object ResetGameEvent:DetailsScreenEvents
    object ShowAnswersDialogEvent:DetailsScreenEvents

    object ShowInfoDialogEvent:DetailsScreenEvents
    object ShowRankingDialogEvent:DetailsScreenEvents
    object LoadPuzzleEvent:DetailsScreenEvents

    object DismissActiveDialogEvent:DetailsScreenEvents

    object SentinielEvent:DetailsScreenEvents
}

sealed interface PuzzleDetailsDialog:Parcelable{
    @Parcelize
    object ConfirmResetDialog:PuzzleDetailsDialog

    @Parcelize
    object InfoDialog:PuzzleDetailsDialog

    @Parcelize
    @JvmInline
    value class RankingDialog(val maximumPuzzleScore:Int):PuzzleDetailsDialog

    @Parcelize
    data class AnswersDialog(val answers:List<String>,
        val found:Set<String>, val pangrams:Set<String>):PuzzleDetailsDialog
}
@Parcelize
sealed class DetailsScreenState:Parcelable{
    object DetailsScreenLoading:DetailsScreenState()
    @Immutable
    data class DetailsScreenData(val centerLetter:Char,
        val outerLetters:List<Char>,
        val currentWord:String,
        val discoveredWords:Set<String>,
        val discoveredPangrams:Set<String>,
        val currentRank:PuzzleRanking,
        val currentScore:Int,
        val activeDialog:PuzzleDetailsDialog? =null,
        val activeWordToast: WordToast?):DetailsScreenState(){
            companion object{
                fun PuzzleBoardState.toDetailsScreenData(activeWordToast:WordToast?=null,
                                                         activeDialog:PuzzleDetailsDialog?=null):DetailsScreenData{
                    return DetailsScreenData(centerLetter =puzzle.centerLetter,
                        outerLetters = gameState.outerLetters,
                        currentWord =gameState.currentWord,
                        discoveredWords =gameState.discoveredWords,
                        discoveredPangrams =gameState.discoveredWords.filter {answer->  answer in puzzle.pangrams }.toSet(),
                        currentRank =currentUserRank,
                        currentScore =currentUserScore,
                        activeWordToast = activeWordToast,
                      activeDialog=activeDialog)
                }
            }
        }
}
sealed interface WordToast:Parcelable{
    @Parcelize
    data class Success(val pointValue: Int) : WordToast

    @Parcelize
    data class Error(val wordError: WordError) : WordToast
}