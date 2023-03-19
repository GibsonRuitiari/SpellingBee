package me.gibsoncodes.spellingbee.puzzlegenerator

import android.os.Handler
import android.util.Log
import androidx.annotation.OpenForTesting
import me.gibsoncodes.spellingbee.di.AndroidComponent
import me.gibsoncodes.spellingbee.utils.getScoreOfWord
import me.gibsoncodes.spellingbee.utils.ifDebugDo
import me.gibsoncodes.spellingbee.utils.shuffle
import java.io.InputStream
import java.text.DateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

interface PuzzleGenerator {
    fun generatePuzzle(count:Int):Puzzle?
}


class PuzzleGeneratorDelegate constructor(private val androidComponent: AndroidComponent):PuzzleGenerator{

    private val ioThreadHandler = Handler(androidComponent.handlerThread.looper)

    companion object{
        private const val MinimumWords =4
        private const val Bingo =7
        private const val WordsTextFile ="words.txt"
        private const val PuzzleGeneratorTag ="PuzzleGeneratorTag"
    }

    override fun generatePuzzle(count: Int): Puzzle? {

        val countDownLatch = CountDownLatch(1)
        val generatedPuzzles = arrayOfNulls<Puzzle>(1)
        ioThreadHandler.post {
            val wordsToUse = androidComponent.assets.open(WordsTextFile).readWordsFromFile()
            val letterPool =generateLetterPoolFromWords(wordsToUse)
            generatedPuzzles[0] = innerGeneratePuzzle(wordsToUse, letterPool)
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        }catch (_:Exception){}

        ifDebugDo { Log.d(PuzzleGeneratorTag,"generated puzzle ${generatedPuzzles[0]}") }

        val puzzleGenerated = generatedPuzzles[0]

        return if (puzzleGenerated!=null && puzzleGenerated.solutions.count() >= 10 && puzzleGenerated.pangrams.isNotEmpty()){
             puzzleGenerated
        }else if (count < 4){
            generatePuzzle(count+1)
        }else  null
    }

    private fun innerGeneratePuzzle(wordsToUse:List<String>,letterPool: Set<String>):Puzzle{
        val partialPuzzle = generatePuzzleFromLetterPool(letterPool)
        return generatePuzzleSolutionsFromWords(partialPuzzle, wordsToUse)
    }
    private fun generatePuzzleSolutionsFromWords(partialPuzzle: Puzzle,
                                                 words: List<String>):Puzzle{
        val letterPool = partialPuzzle.optionalCharacters.joinToString("").plus(partialPuzzle.requiredChar)
        val solutionsWithScore=words.filter {word-> word.all { it in letterPool } && partialPuzzle.requiredChar in word }
            .map {word->
               val score= word.getScoreOfWord()
               word to score
            }.toSet()
        val pangrams = solutionsWithScore.filter { it.second == it.first.length.plus(Bingo) }.map { it.first }.toSet()
        val scores=solutionsWithScore.sumOf { it.second }
        val solutions=solutionsWithScore.map { it.first }.toSet()
        return partialPuzzle.copy(solutions = solutions, pangrams = pangrams, totalScore = scores)
    }

    private fun generatePuzzleFromLetterPool(letterPool:Set<String>):Puzzle{
        val randomizedLetterPool = letterPool.random().shuffle()
        val requiredLetter = randomizedLetterPool.random()
        val optionalLetters = randomizedLetterPool.filter { it!= requiredLetter }.toCharArray().toSet()
        return Puzzle(requiredChar = requiredLetter, optionalCharacters = optionalLetters, solutions = emptySet(),
            totalScore = 0, pangrams = emptySet(), generatedTime = getCurrentDateInString())
    }
    private fun getCurrentDateInString():String{
        val date=Date().time
        val pattern=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM)
        return pattern.format(date)
    }

    private fun generateLetterPoolFromWords(words:List<String>):Set<String>{
       return words.mapNotNull {word->
           val uniqueCharacters = word.toSet()
           if (uniqueCharacters.count()== Bingo) uniqueCharacters.joinToString("")
           else null
       }.toSet()
    }

    private fun InputStream.readWordsFromFile():List<String>{
        val words = mutableListOf<String>()
        bufferedReader().forEachLine {
            val sanitizedWord= it.trim()
            if (sanitizedWord.length >= MinimumWords) words.add(sanitizedWord)
        }
        return words
    }
}