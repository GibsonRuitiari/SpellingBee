package me.gibsoncodes.spellingbee.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateEntity

@Parcelize
data class PuzzleGameState(val puzzleId:Long,val outerLetters:List<Char>,val currentWord:String,
val discoveredWords:Set<String>):Parcelable{
    companion object{
        fun PuzzleUi.blankGameState(): PuzzleGameState = PuzzleGameState(puzzleId = id, outerLetters = outerLetters.toList(),
            currentWord = "", discoveredWords = emptySet())
        fun blankGameState(puzzleId: Long,outerLetters: List<Char>) = PuzzleGameState(puzzleId = puzzleId,
        outerLetters = outerLetters, discoveredWords = emptySet(), currentWord = ""
        )
    }
}

fun PuzzleGameState.toPuzzleGameStateEntity(id: Long): PuzzleGameStateEntity {
    return PuzzleGameStateEntity(
        puzzleId = id,
        outerLetters = outerLetters,
        currentWord = currentWord,
        discoveredWords = discoveredWords
    )
}


