package me.gibsoncodes.spellingbee.ui

import androidx.annotation.StringRes
import me.gibsoncodes.spellingbee.R


sealed interface WordResult {
    object ValidWord: WordResult
    data class BadWord(val errorType: WordError): WordResult
}

enum class WordError(@StringRes val errorMessage:Int){
    TooShort(R.string.puzzle_detail_word_error_too_short),
    TooLong(R.string.puzzle_detail_word_error_too_long),
    MissingCenterLetter(R.string.puzzle_detail_word_error_missing_center_letter),
    AlreadyFound(R.string.puzzle_detail_word_error_already_found),
    NotInWordList(R.string.puzzle_detail_word_error_not_in_word_list),
}