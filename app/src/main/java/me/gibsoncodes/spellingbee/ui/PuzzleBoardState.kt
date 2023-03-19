package me.gibsoncodes.spellingbee.ui

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

@Parcelize
@Immutable
data class PuzzleBoardState(val puzzle: PuzzleUi, val gameState: PuzzleGameState):Parcelable{

    companion object{
        private val pattern=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM)
        val comparator= object :Comparator<PuzzleBoardState>{
            override fun compare(o1: PuzzleBoardState?, o2: PuzzleBoardState?): Int {
                if (o1==null || o2==null) return 0
                val firstTime=pattern.parse(o1.puzzle.generatedTime)
                val secondTime = pattern.parse(o2.puzzle.generatedTime)
                return secondTime?.compareTo(firstTime) ?: 0
               // return firstTime?.compareTo(secondTime) ?: 0
            }
        }
    }

    val currentUserScore:Int
    get() = gameState.discoveredWords.sumOf {userSolution->
        puzzle.getScoreOfWord(userSolution)
    }
    private val currentUserPercentage:Int
    get() = (currentUserScore.div(puzzle.maximumPuzzleScore.toDouble()) ).times(100).roundToInt()

    val currentUserRank: PuzzleRanking
    get() {
       return PuzzleRanking.values()
            .filter {rank-> rank.percentageCutOff <= currentUserPercentage }
            .maxByOrNull { rank-> rank.percentageCutOff } ?: PuzzleRanking.Beginner
    }
    fun checkWordIfValid(word:String):WordResult{
        return when{
            word.length < PuzzleUi.MIN_WORD_LENGTH -> WordResult.BadWord(WordError.TooShort)
            word.length > PuzzleUi.MAXIMUM_WORD_LENGTH -> WordResult.BadWord(WordError.TooLong)
            !word.contains(puzzle.centerLetter) -> WordResult.BadWord(WordError.MissingCenterLetter)
            word !in puzzle.answers -> WordResult.BadWord(WordError.NotInWordList)
            word in gameState.discoveredWords -> WordResult.BadWord(WordError.AlreadyFound)
            else-> WordResult.ValidWord
        }
    }
}