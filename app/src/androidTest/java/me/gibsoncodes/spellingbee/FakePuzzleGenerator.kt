package me.gibsoncodes.spellingbee

import me.gibsoncodes.spellingbee.puzzlegenerator.Puzzle
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator

object FakePuzzleGenerator :PuzzleGenerator{
    override fun generatePuzzle(count: Int): Puzzle {
        return Puzzle(requiredChar = 'W',
            optionalCharacters = setOf('E','F','D','R','O','L'),
            pangrams = setOf("WORD"),
            solutions = setOf("WOOLER","WOOL","LOWER","LEWD","WORD"),
            generatedTime = "18 Mar 2023 23:49:59",
            totalScore = 200
        )
    }
}