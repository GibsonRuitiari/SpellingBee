package me.gibsoncodes.spellingbee

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
    private lateinit var dependencyContainer:DefaultDependencyContainer
    @Before
    fun setUp(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        dependencyContainer = DefaultDependencyContainer.constructNewDependencyContainerInstance()
        dependencyContainer.registerBinding(PuzzleGenerator::class,PuzzleGeneratorDelegate::class,
            (appContext.applicationContext as SpellingBeeApplication).handlerThread.looper,
            appContext.assets)
        puzzleGenerator=dependencyContainer.resolveBinding<PuzzleGenerator>()
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
        dependencyContainer.dispose()
    }
}