package me.gibsoncodes.spellingbee.ui.detailspage

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.gibsoncodes.spellingbee.R
import me.gibsoncodes.spellingbee.ui.navigation.SpellingBeeScreens
import me.gibsoncodes.spellingbee.utils.ModifiedAnimatedContent
import me.gibsoncodes.spellingbee.utils.ifDebugDo


@Composable
fun DetailsScreenUiContent(modifier: Modifier=Modifier,
                           detailsScreenState: DetailsScreenState,
                           onEvent: (DetailsScreenEvents) -> Unit,
                           navigateTo:(SpellingBeeScreens)->Unit){
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        visible.value = true
    }

    BackHandler {
        onEvent(DetailsScreenEvents.PersistGameState)
        navigateTo(SpellingBeeScreens.MainPage)
    }

    ModifiedAnimatedContent(visible = visible.value) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues()),
            topBar = {
                DetailsScreenAppBar(modifier = Modifier, onBackButtonClicked= {
                    onEvent(DetailsScreenEvents.PersistGameState)
                    navigateTo(it) },
                    onInformationIconPressed ={onEvent(DetailsScreenEvents.ShowInfoDialogEvent)},
                    onPuzzleAnswersIconPressed ={onEvent(DetailsScreenEvents.ShowAnswersDialogEvent)},
                    onResetPuzzleIconPressed ={onEvent(DetailsScreenEvents.ShowConfirmResetDialogEvent)})
            }, backgroundColor = Color.White.copy(alpha = 0.4f)
        ) {
            Surface (modifier = Modifier.padding(it)){
                PuzzleDetailsView(puzzleState = detailsScreenState,onEvent)
            }
        }
    }
}


@Composable
fun PuzzleDetailsView(puzzleState:DetailsScreenState,
                      onEvent: (DetailsScreenEvents) -> Unit){
    when(puzzleState){
        is DetailsScreenState.DetailsScreenData ->{
            PuzzleBoard(screenData = puzzleState, onEvent = onEvent)
            puzzleState.activeDialog?.let {activeDialog->
                ShowDialog(activeDialog, onEvent)
            }
        }
        DetailsScreenState.DetailsScreenLoading -> LoadingComponent()
    }
}

@Composable
fun ShowDialog(activeDialog:PuzzleDetailsDialog,
               onEvent:(DetailsScreenEvents) -> Unit){
    when(activeDialog){
        is PuzzleDetailsDialog.InfoDialog-> {
            GameInformationDialog { onEvent(DetailsScreenEvents.DismissActiveDialogEvent) }
        }
        is PuzzleDetailsDialog.RankingDialog->{

            GameRankingDialog(activeDialog.maximumPuzzleScore, onDismiss = {onEvent(DetailsScreenEvents.DismissActiveDialogEvent)})
        }
        is PuzzleDetailsDialog.ConfirmResetDialog->{
            ResetGameConfirmationDialog(onDismissConfirmationDialog = {onEvent(DetailsScreenEvents.DismissActiveDialogEvent)},
                onResetGameConfirmed ={
                    onEvent(DetailsScreenEvents.ResetGameEvent)
                })
        }
        is PuzzleDetailsDialog.AnswersDialog->{
            GameAnswersDialog(activeDialog.answers,
                activeDialog.found,activeDialog.pangrams, onDismiss = {onEvent(DetailsScreenEvents.DismissActiveDialogEvent)})
        }
    }
}
@Composable
fun PuzzleBoard(modifier: Modifier=Modifier,
                screenData:DetailsScreenState.DetailsScreenData,
                onEvent:(DetailsScreenEvents)->Unit){
    var isDiscoveredWordBoxExpanded by remember { mutableStateOf(false) }
    val localView = LocalView.current

    Box(modifier = modifier, contentAlignment = Alignment.Center){
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.SpaceEvenly){
            ScoreBox(screenData.currentRank,screenData.currentScore,
                onScoreBarClicked = {onEvent(DetailsScreenEvents.ShowRankingDialogEvent)})

            DiscoveredWordBox(words =screenData.discoveredWords,
                pangrams = screenData.discoveredPangrams,
                expanded = isDiscoveredWordBoxExpanded,
                toggleWordBoxExpanded = { isDiscoveredWordBoxExpanded=!isDiscoveredWordBoxExpanded })

            WordToastRow(modifier = Modifier.align(Alignment.CenterHorizontally),
                activeWordToast = screenData.activeWordToast,
                dismissActiveWordToast = {onEvent(DetailsScreenEvents.DismissActiveToastEvent)})

            AnswerInputBox(modifier=Modifier.align(Alignment.CenterHorizontally),
                centerLetter = screenData.centerLetter,
                word = screenData.currentWord)
            PuzzleKeyboard(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxHeight(0.75f),
                requiredChar = screenData.centerLetter,
                optionalLetters = screenData.outerLetters.toList(),
                onLetterPressed = {
                    localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onEvent(DetailsScreenEvents.KeyPressEvent(it)) }
            )

            ActionBars(modifier =Modifier.align(Alignment.CenterHorizontally),
                deleteCharButtonClicked ={
                   onEvent(DetailsScreenEvents.DeleteKeypadEvent(screenData.currentWord))
                },
                shuffleCharsButtonClicked = { onEvent(DetailsScreenEvents.ShuffleKeypadEvent(screenData.outerLetters)) },
                enterAnswerButtonClicked = {onEvent(DetailsScreenEvents.EnterKeypadEvent(screenData.currentWord))})
        }
    }
}

@Composable
fun LoadingComponent(){
    Box(
        modifier =Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(40.dp))
    }
}
@Composable
fun DetailsScreenAppBar(modifier: Modifier=Modifier,
                        onInformationIconPressed:()->Unit,
                        onResetPuzzleIconPressed:()->Unit,
                        onPuzzleAnswersIconPressed:()->Unit,
                        onBackButtonClicked:(SpellingBeeScreens)->Unit){
    TopAppBar(modifier = modifier, title = { Text("Spelling Bee", color = Color.Black) }, actions = {
        IconButton(onClick = onInformationIconPressed){
            Icon(Icons.Outlined.Info,contentDescription = stringResource(R.string.puzzle_detail_toolbar_info))
        }
        IconButton(onClick =onResetPuzzleIconPressed){
            Icon(
                Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_puzzle),
                modifier = Modifier.padding(horizontal = 8.dp))
        }
        IconButton(onClick =onPuzzleAnswersIconPressed){
            Icon(Icons.Default.Extension,contentDescription = stringResource(R.string.puzzle_detail_toolbar_answers))
        }

    },backgroundColor = Color.White, navigationIcon = {
        Icon(Icons.Default.ArrowBackIos,modifier = Modifier.padding(horizontal = 8.dp).clickable {
            onBackButtonClicked(SpellingBeeScreens.MainPage)
        },contentDescription = stringResource(R.string.go_back))
    })
}




