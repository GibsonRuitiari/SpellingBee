package me.gibsoncodes.spellingbee.ui

import androidx.annotation.StringRes
import me.gibsoncodes.spellingbee.R

enum class PuzzleRanking(val percentageCutOff:Int,@StringRes val displayText:Int) {
    Beginner(
        percentageCutOff = 0,
        displayText = R.string.puzzle_rank_beginner
    ),
    GoodStart(
        percentageCutOff = 2,
        displayText = R.string.puzzle_rank_goodstart
    ),
    MovingUp(
        percentageCutOff = 5,
        displayText = R.string.puzzle_rank_movingup
    ),
    Good(
        percentageCutOff = 8,
        displayText = R.string.puzzle_rank_good
    ),
    Solid(
        percentageCutOff = 15,
        displayText = R.string.puzzle_rank_solid
    ),
    Nice(
        percentageCutOff = 25,
        displayText = R.string.puzzle_rank_nice
    ),
    Great(
        percentageCutOff = 40,
        displayText = R.string.puzzle_rank_great
    ),
    Amazing(
        percentageCutOff = 50,
        displayText = R.string.puzzle_rank_amazing
    ),
    Genius(
        percentageCutOff = 70,
        displayText = R.string.puzzle_rank_genius
    ),
    QueenBee(
        percentageCutOff = 100,
        displayText = R.string.puzzle_rank_queen_bee
    );

    companion object {
        val sortedValues: List<PuzzleRanking> = values().sortedBy(PuzzleRanking::percentageCutOff)
    }
}