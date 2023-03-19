package me.gibsoncodes.spellingbee.persistence

import me.gibsoncodes.spellingbee.ui.PuzzleUi

data class PuzzleEntity(val id:Long=0,val requiredChar:Char, val optionalCharacters:Set<Char>, val solutions:Set<String>,
                        val totalScore:Int, val pangrams:Set<String>,
    val generatedTime:String){
    companion object{
        fun PuzzleEntity.toPuzzleUi() = PuzzleUi(id = id,
            centerLetter = requiredChar, answers = solutions,
            pangrams = pangrams, generatedTime = generatedTime,
            totalScore = totalScore, outerLetters = optionalCharacters)
    }
}