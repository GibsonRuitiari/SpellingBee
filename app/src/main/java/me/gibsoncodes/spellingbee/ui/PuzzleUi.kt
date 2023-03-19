package me.gibsoncodes.spellingbee.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.gibsoncodes.spellingbee.persistence.PuzzleEntity
import me.gibsoncodes.spellingbee.puzzlegenerator.Puzzle
import me.gibsoncodes.spellingbee.utils.getScoreOfWord

@Parcelize
class PuzzleUi(val id:Long, val centerLetter:Char, val outerLetters:Set<Char>,
val pangrams:Set<String>, val answers:Set<String>,val totalScore:Int,
               val generatedTime:String):Parcelable{
    companion object{
        private const val DefaultID=0L
        const val MIN_WORD_LENGTH = 4
        const val MAXIMUM_WORD_LENGTH =19 // prevents words collocation
        fun Puzzle.toPuzzleUi(): PuzzleUi {
            return PuzzleUi(id = DefaultID, centerLetter = this.requiredChar,
                outerLetters = this.optionalCharacters, pangrams = this.pangrams,
                totalScore=totalScore, generatedTime = generatedTime,
                answers = this.solutions)
        }
        fun PuzzleUi.toPuzzleEntity(): PuzzleEntity = PuzzleEntity(id, centerLetter,outerLetters,answers, totalScore, pangrams, generatedTime)
    }
    val maximumPuzzleScore:Int
    get() = answers.sumOf(::getScoreOfWord)


    fun getScoreOfWord(word:String):Int  = word.getScoreOfWord()


    fun Char.isCharacterEligible():Boolean = centerLetter == this || this in outerLetters

}