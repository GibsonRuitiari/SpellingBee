package me.gibsoncodes.spellingbee.persistence

import android.util.Log
import me.gibsoncodes.spellingbee.persistence.PuzzleEntity.Companion.toPuzzleUi
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateEntity.Companion.toPuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState.Companion.blankGameState
import me.gibsoncodes.spellingbee.ui.PuzzleUi
import me.gibsoncodes.spellingbee.ui.PuzzleUi.Companion.toPuzzleEntity
import me.gibsoncodes.spellingbee.ui.toPuzzleGameStateEntity
import me.gibsoncodes.spellingbee.utils.ifDebugDo

interface PuzzleRepository {
   fun deleteCachedPuzzleById(puzzleId:Long)
   fun insertOrReplacePuzzleGameState(puzzleGameState: PuzzleGameState,puzzleId:Long)
   fun insertOrIgnorePuzzle(puzzleUi: PuzzleUi)
   fun refreshCachedPuzzleBoardStates()
    fun updateGameState(puzzleGameState: PuzzleGameState,puzzleId: Long)
   fun getPuzzleById(puzzleId: Long):PuzzleUi?
   fun getGameStateById(puzzleId: Long):PuzzleGameState?
   fun getCachedPuzzleBoardStates():Set<PuzzleBoardState>
}

class PuzzleRepositoryDelegate constructor(private val puzzleDao:PuzzleDao):PuzzleRepository{
    companion object{
        const val PuzzleRepoLog = "PuzzleRepoLogTag"
    }
    override fun deleteCachedPuzzleById(puzzleId: Long) {
       puzzleDao.deleteCachedPuzzleById(puzzleId)
    }

    override fun updateGameState(puzzleGameState: PuzzleGameState,puzzleId: Long) {
        puzzleDao.updateGameState(puzzleGameState = puzzleGameState
            .toPuzzleGameStateEntity(puzzleId))
    }

    override fun insertOrReplacePuzzleGameState(puzzleGameState: PuzzleGameState,puzzleId: Long) {
        puzzleDao.insertOrReplacePuzzleGameState(puzzleGameState.toPuzzleGameStateEntity(puzzleId))
    }

    override fun refreshCachedPuzzleBoardStates() {
        puzzleDao.requeryCachedPuzzleBoardStates()
    }

    override fun getGameStateById(puzzleId: Long): PuzzleGameState? {
        return puzzleDao.getGameStateByPuzzleId(puzzleId)?.toPuzzleGameState()
    }

    override fun getPuzzleById(puzzleId: Long): PuzzleUi? {
       return puzzleDao.getPuzzleById(puzzleId)?.toPuzzleUi()
    }

    override fun insertOrIgnorePuzzle(puzzleUi: PuzzleUi) {
        puzzleDao.insertOrIgnorePuzzleEntity(puzzleUi.toPuzzleEntity())
    }

    override fun getCachedPuzzleBoardStates(): Set<PuzzleBoardState> {
       return puzzleDao.getCachedPuzzleBoardStates().map {puzzleBoardStateEntity->
            val puzzleUi = puzzleBoardStateEntity.puzzleEntity.toPuzzleUi()
            val gameState=puzzleBoardStateEntity.gameStateEntity?.toPuzzleGameState() ?: puzzleUi.blankGameState()
           ifDebugDo { Log.d(PuzzleRepoLog,"game state  $gameState  | puzzleUi $puzzleUi") }
           PuzzleBoardState(puzzle = puzzleUi, gameState = gameState)
        }.toSet()
    }

}