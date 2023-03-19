package me.gibsoncodes.spellingbee.ui.mainpage

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.gibsoncodes.spellingbee.R
import me.gibsoncodes.spellingbee.persistence.PuzzleRepository
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.ui.PuzzleBoardState
import me.gibsoncodes.spellingbee.ui.PuzzleRanking
import me.gibsoncodes.spellingbee.utils.DismissableComposable
import me.gibsoncodes.spellingbee.utils.ModifiedAnimatedContent



@Composable
fun MainScreenView(mainScreenState: MainScreenState,
                   onPuzzleClicked: (puzzleId: Long) -> Unit,
                   onEvent:(MainScreenEvents)->Unit){
    when(mainScreenState){
      is MainScreenState.MainScreenData->{
          val puzzleBoardStates=mainScreenState.puzzleBoardStates
          if (puzzleBoardStates.isEmpty()){
              EmptyScreenComponent{
                  onEvent(MainScreenEvents.GeneratePuzzle)
              }
          }else{
              val displayList=puzzleBoardStates.reversed()
              MainScreenPuzzleBoardStateList(puzzleBoardStates = displayList, onGeneratePuzzle = {  onEvent(MainScreenEvents.GeneratePuzzle)},
                  onPuzzleDelete = {
                  onEvent(MainScreenEvents.OnDeletePuzzle(it))
              }, onPuzzleClicked = onPuzzleClicked)
          }
      }
       is MainScreenState.MainScreenError->{
           Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){
               Text("The following error occurred ${mainScreenState.errorMessage}", textAlign = TextAlign.Center)
               Spacer(Modifier.height(20.dp))
               Button(onClick = {
                   onEvent(MainScreenEvents.LoadCachedPuzzleBoardStates)
               }){
                   Text("Refresh")
               }
           }
       }

       MainScreenState.MainScreenLoading->{
           LoadingComponent()
       }
    }
}

@Composable
fun MainScreenPuzzleBoardStateList(modifier:Modifier=Modifier,
                                   puzzleBoardStates:List<PuzzleBoardState>,
                                   onPuzzleClicked:(puzzleId:Long)->Unit,
                                   onPuzzleDelete:(puzzleId:Long)->Unit,
                                   onGeneratePuzzle:()->Unit) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        visible.value = true
    }

    ModifiedAnimatedContent(visible = visible.value,enterAnimation = slideInHorizontally { -it/2 }+ fadeIn()) {
        Scaffold(modifier = modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
            topBar = {
                TopAppBar(title = { Text("Spelling Bee", color = Color.Black) }, actions = {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.generate_puzzle),
                        modifier = Modifier.padding(horizontal = 8.dp).clickable {
                            onGeneratePuzzle()
                        })
                }, backgroundColor = Color.White)
            }, backgroundColor = Color.White.copy(alpha = 0.4f)
        ) { _ ->

            /* for some reason the lazy column keeps getting close to the app-bar so we add content-padding to prevent that */
            LazyColumn(
                contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(puzzleBoardStates) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onPuzzleClicked(it.puzzle.id) },
                        shape = RoundedCornerShape(4.dp), elevation = 2.dp
                    ) {
                        DismissableComposable(item = it,
                            background = { DeletePuzzleBoardStateRow() },
                            foreground = {PuzzleRow(puzzleBoarState = it)},
                            onDismissed = {
                                println("dismissed puzzle id ${it.puzzle.id}")
                                onPuzzleDelete(it.puzzle.id)})

                    }
                }
            }
        }
    }
}

@Composable
fun PuzzleRow(modifier: Modifier=Modifier,puzzleBoarState:PuzzleBoardState){
    Surface(modifier = modifier){
        Column(modifier = Modifier.padding(12.dp)){
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text( buildAnnotatedString {
                    puzzleBoarState.puzzle.centerLetter
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                        append(puzzleBoarState.puzzle.centerLetter)
                    }
                    append(puzzleBoarState.puzzle.outerLetters.joinToString(""))
                }, modifier = Modifier.weight(1f),
                    fontSize = 24.sp, maxLines = 1, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black)
                RankLabel(puzzleBoarState.currentUserRank,puzzleBoarState.currentUserScore)
            }
            Spacer(modifier = Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically){
                Icon(painterResource(R.drawable.baseline_casino_24),contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(puzzleBoarState.puzzle.generatedTime, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
fun DeletePuzzleBoardStateRow() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.error)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.DeleteForever,
            contentDescription = null,
            tint = MaterialTheme.colors.onError
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.delete),
            color = MaterialTheme.colors.onError, fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun RankLabel(rank: PuzzleRanking, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = rank.displayText),
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.width(4.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .wrapContentSize()
                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = score.toString(),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
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
fun EmptyScreenComponent(generateNewPuzzle:()->Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()
        .padding(WindowInsets.systemBars.asPaddingValues())
        ,topBar = {
        TopAppBar(title = { Text("Spelling Bee") },
            actions = {
                Icon(Icons.Default.Add,null, modifier = Modifier.padding(horizontal = 8.dp).clickable {
                    generateNewPuzzle()
                })
            }, backgroundColor = Color.White)
    }, backgroundColor = Color.White.copy(alpha = 0.4f)){
        Box(modifier = Modifier.fillMaxSize().padding(it),
            contentAlignment = Alignment.Center){
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
                    .padding(it),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(id =R.drawable.baseline_cruelty_free_24),
                    contentDescription = null,
                )
                Spacer(Modifier.size(16.dp))
                Text(
                    stringResource(R.string.puzzle_list_empty_state),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
