package me.gibsoncodes.spellingbee.persistence

import android.provider.BaseColumns

object PuzzleContract {
    object PuzzleEntry:BaseColumns{
        const val PuzzleTableOutLetterColumnName ="outerLetters"
        const val PuzzleTableInnerLetterColumnName="innerLetter"
        const val PuzzleTablePangramColumnName="pangrams"
        const val PuzzleTableSolutionsColumnName="answers"
        const val PuzzleTableGeneratedTimeColumnName="generatedAt"
        const val PuzzleTableTotalScoreColumnName="totalScore"
        const val PuzzleTableCenterLetterOuterLetterIndexColumnName="index_puzzles_centerLetter_outerLetters"
    }
}