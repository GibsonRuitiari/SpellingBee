package me.gibsoncodes.spellingbee

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import me.gibsoncodes.spellingbee.di.*
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleGeneratorTest {
    private var puzzleGenerator:PuzzleGenerator?=null
    private lateinit var factoryManager:FactoryManager
    @Before
    fun setUp(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        factoryManager =DependenciesContainer.factoryManager
        factoryManager.onActivityCreate((appContext as? Activity)?.lastNonConfigurationInstance as? InstancesCache)
        puzzleGenerator=PuzzleGeneratorDelegate((appContext.applicationContext as SpellingBeeApplication).handlerThread.looper,
            appContext.assets)
    }

    @Test
    fun testThatPuzzlesAreBeingGenerated(){
        val generatedPuzzle=puzzleGenerator?.generatePuzzle(0)
        assertThat(generatedPuzzle).isNotNull()
        // test that the generated puzzles are valid: valid puzzles are those that have more than 10 solutions
        assertThat(generatedPuzzle?.solutions?.count()).isGreaterThan(10)
    }

    @After
    fun tearDown(){
        puzzleGenerator =null
        factoryManager.onActivityDestroy()
    }
}