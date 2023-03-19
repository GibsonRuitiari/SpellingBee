package me.gibsoncodes.spellingbee.persistence

import android.content.ContentValues
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateCurrentWordColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateIdColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateOuterLettersColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateSolutionColumnName
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.utils.ifDebugDo

data class PuzzleGameStateEntity(val puzzleId:Long,
                                 val currentWord:String,
                                 val discoveredWords:Set<String>,
                                 val outerLetters:List<Char>){
    companion object{
        fun PuzzleGameStateEntity.toContentValues(id:Long?=null) :ContentValues{
            ifDebugDo { println("discovered words inside content value received $discoveredWords") }
            val solution =if (discoveredWords.isEmpty()) "" else discoveredWords.joinToString(",")
            val contentValues = ContentValues().apply {
                id?.let {_puzzleId-> put(PuzzleGameStateIdColumnName,_puzzleId.toInt()) }

                put(PuzzleGameStateSolutionColumnName,solution)
                put(PuzzleGameStateCurrentWordColumnName,currentWord)
                put(PuzzleGameStateOuterLettersColumnName,outerLetters.joinToString(","))
            }
            return contentValues
        }
        fun PuzzleGameStateEntity.toPuzzleGameState(): PuzzleGameState = PuzzleGameState(puzzleId, outerLetters, currentWord, discoveredWords)
    }
}