package me.gibsoncodes.spellingbee.persistence

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.HandlerThread
import android.provider.BaseColumns
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.sqlite.transaction
import me.gibsoncodes.spellingbee.log.debug
import me.gibsoncodes.spellingbee.log.info
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper.Companion.PuzzleGameStateTableName
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper.Companion.PuzzleTableName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableGeneratedTimeColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableInnerLetterColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableOutLetterColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTablePangramColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableSolutionsColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableTotalScoreColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateCurrentWordColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateIdColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateOuterLettersColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateSolutionColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateEntity.Companion.toContentValues
import me.gibsoncodes.spellingbee.utils.getDatabaseInstance
import me.gibsoncodes.spellingbee.utils.ifDebugDo
import me.gibsoncodes.spellingbee.utils.withLock
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton

interface PuzzleDao:AutoCloseable{
    fun requeryCachedPuzzleBoardStates()
    fun deleteCachedPuzzleById(puzzleId: Long)
    fun getPuzzleById(puzzleId:Long):PuzzleEntity?
    fun getGameStateByPuzzleId(puzzleId: Long):PuzzleGameStateEntity?
    fun updateGameState(puzzleGameState: PuzzleGameStateEntity)
    fun insertOrReplacePuzzleGameState(puzzleGameState:PuzzleGameStateEntity):Long
    fun insertOrIgnorePuzzleEntity(puzzleEntity: PuzzleEntity):Long
    fun getCachedPuzzleBoardStates():List<PuzzleBoardStateEntity>
}

@Singleton
class PuzzleDaoDelegate @Inject constructor(
    sqLiteOpenHelper: SQLiteOpenHelper,
    handlerThread: HandlerThread):PuzzleDao{
    private val database: SQLiteDatabase
    init {
        database= sqLiteOpenHelper.getDatabaseInstance(handlerThread.looper)!!
    }
    private val backgroundThreadHandler=Handler(handlerThread.looper)


    private var gameStateCursor:Cursor?=null
    // a binary semaphore that has only two states: one permit available , 0 permit available
    // used this over re-entrant lock because it allows deadlock recovery
    private var binarySemaphore = java.util.concurrent.Semaphore(1)



    override fun getPuzzleById(puzzleId: Long): PuzzleEntity? {
        val whereArgs = arrayOf("${puzzleId.toInt()}")
        val countDownLatch = CountDownLatch(1)
        val results = arrayOfNulls<PuzzleEntity>(1)
        backgroundThreadHandler.post {
            val puzzleEntity=database.query(PuzzleTableName,null,"${BaseColumns._ID} = ?",whereArgs,null,null,null,"1")
                ?.use {cursor->
              if (cursor.count<0) return@use null
              else{
                  cursor.moveToFirst()
                  cursor.getPuzzleEntityFromColumns()
              }

            }
            results[0] = puzzleEntity

            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

        return results[0]
    }

    override fun updateGameState(puzzleGameState: PuzzleGameStateEntity) {
        val whereArgs = arrayOf("${puzzleGameState.puzzleId.toInt()}")

        val countDownLatch = CountDownLatch(1)
        val contentValues = puzzleGameState.toContentValues(puzzleGameState.puzzleId)

        backgroundThreadHandler.post {
            val numberOfColumnsUpdated=database.update(PuzzleGameStateTableName,contentValues,"$PuzzleGameStateIdColumnName = ?",
                whereArgs)
            ifDebugDo {
                info<PuzzleDaoDelegate> { "number of columns updated $numberOfColumnsUpdated" }}
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

    }
    override fun getGameStateByPuzzleId(puzzleId: Long): PuzzleGameStateEntity? {
        val whereArgs = arrayOf("${puzzleId.toInt()}")
        val countDownLatch = CountDownLatch(1)
        val results = arrayOfNulls<PuzzleGameStateEntity>(1)
        backgroundThreadHandler.post {

            val  gameStateEntity=database.query(PuzzleGameStateTableName,null,"$PuzzleGameStateIdColumnName = ?",
                whereArgs,null,null,null,"1").use {
                it.moveToFirst()
                if (it.count>0){
                    it.getPuzzleGameStateFromCursorColumns()
                }else null
            }
            results[0] = gameStateEntity
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}
        return results[0]
    }
    override fun deleteCachedPuzzleById(puzzleId: Long) {
        val whereArgs = arrayOf("${puzzleId.toInt()}")
        backgroundThreadHandler.post {
            database.transaction {
              val numberOfPuzzleRowsDeleted=  delete(PuzzleTableName,"${BaseColumns._ID} = ?", whereArgs)
              val numberOfPuzzleGameStateRowsDeleted= delete(PuzzleGameStateTableName,"$PuzzleGameStateIdColumnName = ?",whereArgs)
              ifDebugDo {
                  debug<PuzzleDao> { "number of game state rows deleted $numberOfPuzzleGameStateRowsDeleted  number of puzzle row deleted $numberOfPuzzleRowsDeleted" }
              }
            }
        }
    }

    override fun getCachedPuzzleBoardStates(): List<PuzzleBoardStateEntity> {
        val countDownLatch = CountDownLatch(1)
        val puzzleBoardStates = mutableListOf<PuzzleBoardStateEntity>()

        backgroundThreadHandler.post {
            innerGetPuzzleBoardStates().let { puzzleBoardStates.addAll(it) }
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

        return puzzleBoardStates
    }



    override fun insertOrIgnorePuzzleEntity(puzzleEntity: PuzzleEntity): Long {
        val countDownLatch = CountDownLatch(1)
        val results = arrayOfNulls<Long>(1)
        backgroundThreadHandler.post {
            val puzzleId=database.internalInsertPuzzleEntity(puzzleEntity,SQLiteDatabase.CONFLICT_IGNORE)
            ifDebugDo { debug<PuzzleDaoDelegate> {"ID of the inserted puzzle $puzzleId"  } }
            results[0] = puzzleId
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

        return results[0] ?: -1L
    }
    override fun insertOrReplacePuzzleGameState(puzzleGameState: PuzzleGameStateEntity): Long {
        val countDownLatch = CountDownLatch(1)
        val results = arrayOfNulls<Long>(1)
        backgroundThreadHandler.post {
            val _puzzleId=database.internalInsertPuzzleGameState(puzzleGameState,SQLiteDatabase.CONFLICT_REPLACE)
            ifDebugDo { debug<PuzzleDaoDelegate> { "ID of the inserted puzzle game state $_puzzleId" } }

            results[0] = _puzzleId
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

        return results[0] ?: -1L
    }

    private fun SQLiteDatabase.internalInsertPuzzleEntity(puzzle: PuzzleEntity, insertConflictOption: Int):Long{
        val contentValues = ContentValues().apply {
            put(PuzzleTableOutLetterColumnName,puzzle.optionalCharacters.joinToString(","))
            put(PuzzleTableInnerLetterColumnName,puzzle.requiredChar.code)
            put(PuzzleTableSolutionsColumnName,puzzle.solutions.joinToString(","))
            put(PuzzleTablePangramColumnName, puzzle.pangrams.joinToString(","))
            put(PuzzleTableGeneratedTimeColumnName,puzzle.generatedTime)
            put(PuzzleTableTotalScoreColumnName,puzzle.totalScore)
        }
        return insertWithOnConflict(PuzzleTableName, null, contentValues,insertConflictOption)
    }

    private fun SQLiteDatabase.internalInsertPuzzleGameState(gameStateEntity: PuzzleGameStateEntity,
                                                             insertConflictOption: Int):Long{
        val input=gameStateEntity.toContentValues()
        return insertWithOnConflict(PuzzleGameStateTableName,null, input,insertConflictOption)
    }


    private fun innerGetPuzzleBoardStates():List<PuzzleBoardStateEntity>{
        if (gameStateCursor==null){
            ifDebugDo {
                debug<PuzzleDaoDelegate> { "initializing game state cursor initially" }
            }
            gameStateCursor = internalGetPuzzleGameStateCursor()
        }
       return gameStateCursor?.use {
            val numberOfRowsInDb = it.count
            if (numberOfRowsInDb <= 0){
                ifDebugDo { debug<PuzzleDaoDelegate> {  "no cached puzzle board states in the database"} }
                emptyList()

            }else{
                val puzzles = mutableListOf<PuzzleBoardStateEntity>()
                while (it.moveToNext()){
                    val puzzleEntity = it.getPuzzleEntityFromColumns()
                    val puzzleGameStateEntity = it.getPuzzleGameStateFromCursorColumns()
                    puzzleEntity?.let {_puzzle->
                        puzzles.add(PuzzleBoardStateEntity(gameStateEntity = puzzleGameStateEntity, _puzzle))

                    }
                }
                ifDebugDo {
                   debug<PuzzleDaoDelegate> { "${puzzles.size} puzzle board states found in the database" }
                }
                puzzles
            }
        } ?: emptyList()

    }
    private fun Cursor.getPuzzleGameStateFromCursorColumns():PuzzleGameStateEntity? {
        // no game state associated with this puzzle so return null instead
        val gameStateIdIndex = getColumnIndex(PuzzleGameStateIdColumnName)
        val gameStateId = getIntOrNull(gameStateIdIndex)?.toLong()
        return if (gameStateId == null) null
        else {
            val gameStateUserSolutionsIndex = getColumnIndexOrThrow(PuzzleGameStateSolutionColumnName)
            val gameStateCurrentWordIndex = getColumnIndexOrThrow(PuzzleGameStateCurrentWordColumnName)
            val gameStateOuterLettersIndex = getColumnIndexOrThrow(PuzzleGameStateOuterLettersColumnName)
            val gameStateCurrentWord = getString(gameStateCurrentWordIndex)
            val gameStateUserSolutions = getString(gameStateUserSolutionsIndex).split(",").toSet()
            val gameStateOuterLetters = getString(gameStateOuterLettersIndex).split(",").map { it.toSet() }.flatten()
            PuzzleGameStateEntity(
                puzzleId = gameStateId, currentWord = gameStateCurrentWord,
                discoveredWords = gameStateUserSolutions, outerLetters = gameStateOuterLetters
            )
        }
    }

    private fun Cursor.getPuzzleEntityFromColumns(): PuzzleEntity? {
        val puzzleIdIndex = getColumnIndex(BaseColumns._ID)
        val puzzleId = getIntOrNull(puzzleIdIndex)?.toLong()
        return if (puzzleId==null){
            null
        }
        else{
            val outerLettersColumnIndex = getColumnIndexOrThrow(PuzzleTableOutLetterColumnName)
            val innerLetterColumnIndex = getColumnIndexOrThrow(PuzzleTableInnerLetterColumnName)
            val answersColumnIndex =getColumnIndexOrThrow(PuzzleTableSolutionsColumnName)
            val pangramColumnIndex = getColumnIndexOrThrow(PuzzleTablePangramColumnName)
            val totalScoreIndex = getColumnIndexOrThrow(PuzzleTableTotalScoreColumnName)
            val generatedTimeIndex = getColumnIndexOrThrow(PuzzleTableGeneratedTimeColumnName)

            val answers = getString(answersColumnIndex).split(",").toSet()
            val pangrams = getString(pangramColumnIndex).split(",").toSet()
            val innerLetter = getInt(innerLetterColumnIndex).toChar()
            val totalScore = getInt(totalScoreIndex)
            val generatedTime = getString(generatedTimeIndex)
            val outerLetters = getString(outerLettersColumnIndex).split(",").map { it.toSet() }.flatten().toSet()
            return PuzzleEntity(id = puzzleId, solutions = answers, pangrams = pangrams,
                requiredChar = innerLetter, optionalCharacters = outerLetters, totalScore = totalScore,
                generatedTime = generatedTime)
        }

    }

    override fun requeryCachedPuzzleBoardStates() {
        val newCursor = internalGetPuzzleGameStateCursor()
        val oldCursor = gameStateCursor
        if (oldCursor!=null && oldCursor.isClosed.not()) oldCursor.close()
        gameStateCursor = newCursor
    }

    private fun internalGetPuzzleGameStateCursor():Cursor?{
       return database.rawQuery(DatabaseHelper.PuzzleBoardStateEntitySqlStatement,null)
    }

    override fun close() {
        gameStateCursor?.let {cursor->
            binarySemaphore.withLock {
                if (!cursor.isClosed){
                    cursor.close()
                    gameStateCursor=null
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return when{
            this === other ->true
            javaClass!=other?.javaClass->false
            else->{
                other as PuzzleDaoDelegate
                //compare the database paths. They should be pointing to the same path otherwise the  instances are different
                if (other.database.version!=database.version) return false
                else true
            }
        }
    }

    override fun toString(): String {
        return "${this.javaClass.canonicalName} -- ${this.hashCode()}"
    }

    override fun hashCode(): Int {
        var result = database.version.hashCode()
        result = 31 * result + backgroundThreadHandler.hashCode()
        return result
    }

}