package me.gibsoncodes.spellingbee.puzzlegenerator


data class Puzzle (val requiredChar:Char, val optionalCharacters:Set<Char>, val solutions:Set<String>,
    val totalScore:Int, val pangrams:Set<String>, val generatedTime:String)