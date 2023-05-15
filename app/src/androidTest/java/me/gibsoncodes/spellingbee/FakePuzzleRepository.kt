package me.gibsoncodes.spellingbee

import me.gibsoncodes.spellingbee.persistence.PuzzleBoardStateEntity
import me.gibsoncodes.spellingbee.persistence.PuzzleEntity
import me.gibsoncodes.spellingbee.persistence.PuzzleEntity.Companion.toPuzzleUi
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState.Companion.blankGameState
import me.gibsoncodes.spellingbee.ui.PuzzleUi

class FakePuzzleRepository:PuzzleRepository {

    private fun getPuzzleBoardStatesFromPuzzleEntities():List<PuzzleBoardState>{
        val puzzleEntities=List(10){ getDummyPuzzleEntity(it.toLong())}
        return puzzleEntities.map {
           val gameState= getDummyGameState(it.id,it.optionalCharacters.toList())
           PuzzleBoardState(puzzle = it.toPuzzleUi(), gameState=gameState)
        }
    }
    private var puzzleBoardStates:List<PuzzleBoardState> = getPuzzleBoardStatesFromPuzzleEntities()

    private fun getDummyGameState(puzzleId:Long,outerLetters:List<Char>):PuzzleGameState
            = PuzzleGameState.blankGameState(puzzleId, outerLetters)

    private fun getDummyPuzzleEntity(id:Long): PuzzleEntity = PuzzleEntity(id = id, requiredChar = 'W',
        optionalCharacters = setOf('E','F','D','R','O','L'),
        pangrams = setOf("WORD"),
        solutions = setOf("WOOLER","WOOL","LOWER","LEWD","WORD"),
        generatedTime = "18 Mar 2023 23:49:59",
        totalScore = 200
    )
    override fun deleteCachedPuzzleById(puzzleId: Long) {
        val puzzleBoardToBeDeleted=puzzleBoardStates.firstOrNull { it.puzzle.id == puzzleId && it.gameState.puzzleId==puzzleId} ?: return
       puzzleBoardStates=puzzleBoardStates.minus(puzzleBoardToBeDeleted)
    }

    override fun insertOrReplacePuzzleGameState(puzzleGameState: PuzzleGameState, puzzleId: Long) {
        // no op
    }

    override fun insertOrIgnorePuzzle(puzzleUi: PuzzleUi) {
        puzzleBoardStates=puzzleBoardStates.plus(PuzzleBoardState(puzzle = puzzleUi, puzzleUi.blankGameState()))
    }

    override fun refreshCachedPuzzleBoardStates() {
        // nop
    }

    override fun updateGameState(puzzleGameState: PuzzleGameState, puzzleId: Long) {
        val gameStateToBeUpdated=puzzleBoardStates.firstOrNull { it.gameState.puzzleId ==puzzleId } ?: return
        puzzleBoardStates=puzzleBoardStates.plus(gameStateToBeUpdated.copy(gameState = puzzleGameState))
    }

    override fun getPuzzleById(puzzleId: Long): PuzzleUi? {
       return puzzleBoardStates.firstOrNull { it.puzzle.id==puzzleId }?.puzzle
    }

    override fun getGameStateById(puzzleId: Long): PuzzleGameState? {
        return puzzleBoardStates.firstOrNull { it.puzzle.id==puzzleId }?.gameState
    }

    override fun getCachedPuzzleBoardStates(): Set<PuzzleBoardState> {
        return puzzleBoardStates.toSet()
    }

    override fun close() {

    }

}