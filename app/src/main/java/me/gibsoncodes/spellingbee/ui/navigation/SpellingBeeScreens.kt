package me.gibsoncodes.spellingbee.ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SpellingBeeScreens : Parcelable {
    object MainPage:SpellingBeeScreens()
    data class DetailsPage(val puzzleId:Long):SpellingBeeScreens()
}

