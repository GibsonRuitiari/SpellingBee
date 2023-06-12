package me.gibsoncodes.spellingbee

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import me.gibsoncodes.spellingbee.di.*
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper
import me.gibsoncodes.spellingbee.persistence.PuzzleDao
import me.gibsoncodes.spellingbee.persistence.PuzzleDaoDelegate
import me.gibsoncodes.spellingbee.persistence.PuzzleEntity
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState.Companion.blankGameState
import me.gibsoncodes.spellingbee.ui.toPuzzleGameStateEntity
import me.gibsoncodes.spellingbee.utils.getDatabaseInstance
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpellingBeeDatabaseTest {
    private var inMemoryDatabase: SQLiteDatabase?=null
    private var puzzleDaoDelegate:PuzzleDaoDelegate?=null
    private lateinit var dependencyContainer:DependencyContainer
    private val testDatabaseName="testDatabase.db"
    private val testDatabaseVersion=1
    @Before
    fun setUp(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        dependencyContainer = DefaultDependencyContainer.constructNewDependencyContainerInstance()

        dependencyContainer.registerBinding(
            SQLiteOpenHelper::class,
            DatabaseHelper::class,appContext,testDatabaseName,testDatabaseVersion)
        val sqliteOpenHelper=dependencyContainer.resolveBinding(DatabaseHelper::class) as SQLiteOpenHelper
        val handlerThread=(appContext as SpellingBeeApplication).handlerThread

        dependencyContainer.registerBinding(PuzzleDao::class,PuzzleDaoDelegate::class,handlerThread)
        puzzleDaoDelegate=dependencyContainer.resolveBinding(PuzzleDao::class) as PuzzleDaoDelegate

        inMemoryDatabase=sqliteOpenHelper.getDatabaseInstance(appContext.handlerThread.looper)
    }
    @Test
    fun testInMemoryDatabaseInstanceIsAcquiredAndDbIsEmpty(){
        assertThat(inMemoryDatabase).isNotNull()
        assertThat(inMemoryDatabase?.isOpen).isTrue()
        // connection is just acquired so this should return 0 records hence empty
        assertThat(puzzleDaoDelegate?.getCachedPuzzleBoardStates())
            .isEmpty()
    }

    @Test
    fun testPuzzleBoardStateInsertionAndQuery(){
        val puzzleEntity = getDummyPuzzleEntity()
        val affectedRows=puzzleDaoDelegate?.insertOrIgnorePuzzleEntity(puzzleEntity)
        assertThat(affectedRows).isNotEqualTo(-1L)
        val invalidAffectedRow=puzzleDaoDelegate?.insertOrIgnorePuzzleEntity(puzzleEntity)
        // conflict option is ignore so -1L will be returned
        assertThat(invalidAffectedRow).isEqualTo(-1L)
        val blankGameState= getDummyGameState(affectedRows!!,puzzleEntity.optionalCharacters.toList())

        val blankGameStateEntity = blankGameState.toPuzzleGameStateEntity(affectedRows)

        val affectedPuzzleGameStateRow=puzzleDaoDelegate?.insertOrReplacePuzzleGameState(blankGameStateEntity)

        assertThat(affectedPuzzleGameStateRow).isNotEqualTo(-1L)
        // conflict option is replace so returned id won't be -1L
        val newPuzzleGameStateId=puzzleDaoDelegate?.insertOrReplacePuzzleGameState(blankGameStateEntity)
        assertThat(newPuzzleGameStateId).isNotEqualTo(-1L)

        val cachedPuzzleBoardStates=puzzleDaoDelegate?.getCachedPuzzleBoardStates()

        // cannot be empty
        assertThat(cachedPuzzleBoardStates).isNotEmpty()

    }
    @Test
    fun testDeleteCachedPuzzleBoardState(){
        val puzzleEntity = getDummyPuzzleEntity()
        val idOfFirstPuzzle=puzzleDaoDelegate?.insertOrIgnorePuzzleEntity(puzzleEntity)

        val firstBlankGameState= getDummyGameState(idOfFirstPuzzle!!,puzzleEntity.optionalCharacters.toList())

        val blankGameStateEntity = firstBlankGameState.toPuzzleGameStateEntity(idOfFirstPuzzle)

        val firstIdOfInsertedGameState=puzzleDaoDelegate?.insertOrReplacePuzzleGameState(blankGameStateEntity)
        // inserted successfully
        assertThat(firstIdOfInsertedGameState).isNotEqualTo(-1L)

        puzzleDaoDelegate?.deleteCachedPuzzleById(idOfFirstPuzzle)

        // should be null because the puzzle associated with this game state was deleted
        val nullGameState=puzzleDaoDelegate?.getGameStateByPuzzleId(firstIdOfInsertedGameState!!)
        assertThat(nullGameState).isNull()

    }

    private fun getDummyGameState(puzzleId:Long, outerLetters:List<Char>):PuzzleGameState
    = blankGameState(puzzleId,outerLetters)

    private fun getDummyPuzzleEntity():PuzzleEntity = PuzzleEntity(requiredChar = 'W',
        optionalCharacters = setOf('E','F','D','R','O','L'),
        pangrams = setOf("WORD"),
        solutions = setOf("WOOLER","WOOL","LOWER","LEWD","WORD"),
        generatedTime = "18 Mar 2023 23:49:59",
        totalScore = 200
    )

    @After
    fun tearDown(){
        inMemoryDatabase?.releaseReference()
        dependencyContainer.dispose()
    }
}