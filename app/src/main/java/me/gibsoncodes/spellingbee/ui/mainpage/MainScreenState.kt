package me.gibsoncodes.spellingbee.ui.mainpage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleGameState
import me.gibsoncodes.spellingbee.ui.PuzzleUi

@Parcelize
sealed class MainScreenState:Parcelable{
    object MainScreenLoading:MainScreenState()
    data class MainScreenError(val errorMessage:String):MainScreenState()
    data class MainScreenData(val puzzleBoardStates:Set<PuzzleBoardState>):MainScreenState()
}

interface MainScreenEvents{
    @JvmInline
    value class OnDeletePuzzle(val puzzleId:Long):MainScreenEvents
    data class OnInsertPuzzle(val puzzleUi: PuzzleUi):MainScreenEvents
    object RefreshPuzzleBoardStates:MainScreenEvents
    object LoadCachedPuzzleBoardStates:MainScreenEvents
    object GeneratePuzzle:MainScreenEvents
}