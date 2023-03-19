package me.gibsoncodes.spellingbee.ui.detailspage

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import me.gibsoncodes.spellingbee.R
import me.gibsoncodes.spellingbee.ui.PuzzleRanking
import me.gibsoncodes.spellingbee.ui.theme.ANSWER_FOUND
import me.gibsoncodes.spellingbee.utils.ifDebugDo
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt


@Composable
fun GameAnswersDialog(answers:List<String>,
                      found:Set<String>,
                      pangrams:Set<String>,
                      onDismiss: () -> Unit) {
    val puzzleAnswers by remember(answers){ mutableStateOf(answers) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colors.surface
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) {
                item{
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text =stringResource(R.string.puzzle_rules_dialog_answers),
                        fontSize = 24.sp
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
                }
                item { Spacer(modifier = Modifier.size(16.dp))  }
                items(puzzleAnswers) { word ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        val isDiscovered = word in found
                        val isPangram = word in pangrams
                        Icon(
                            imageVector = if (isDiscovered) Icons.Filled.Check else Icons.Filled.Close,
                            tint = if (isDiscovered) ANSWER_FOUND else MaterialTheme.colors.error,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = word,
                            fontSize = 16.sp,
                            fontWeight = if (isPangram) FontWeight.ExtraBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

}


@Composable
fun ScoreBox(rank: PuzzleRanking, score: Int,onScoreBarClicked:()->Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onScoreBarClicked() }
            .padding(bottom = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaxWidthText(
            text = stringResource(rank.displayText),
            options = PuzzleRanking.sortedValues.map {
                stringResource(it.displayText)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colors.secondary)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sortedRanks = PuzzleRanking.sortedValues
                val indexOfRank = sortedRanks.indexOf(rank)
                sortedRanks.forEachIndexed { index, puzzleRanking ->
                    val bubbleSize = if (puzzleRanking == rank) 24.dp else 8.dp
                    val color =
                        if (index <= indexOfRank) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
                    Surface(
                        modifier = Modifier
                            .wrapContentSize()
                            .defaultMinSize(minWidth = bubbleSize, minHeight = bubbleSize),
                        shape = CircleShape,
                        color = color,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (puzzleRanking == rank) {
                                Text(
                                    text = score.toString(),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MaxWidthText(
    text: String,
    options: List<String> = emptyList(),
    fontWeight: FontWeight = FontWeight.Bold
) {
    Layout(
        content = {
            setOf(text).plus(options).forEach { string ->
                Text(string, fontWeight = fontWeight)
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minHeight = 0))
        }
        val maxWidth: Int = placeables.maxOf(Placeable::width)
        layout(maxWidth, constraints.minHeight) {
            val item = placeables.first()
            item.place(0, -item.height / 2)
        }
    }
}

@Composable
fun DiscoveredWordBox(
    words: Set<String>,
    pangrams: Set<String>,
    expanded: Boolean,
    toggleWordBoxExpanded:()->Unit,
) {
    // technically discovered words are saved with "" placeholder instead of null so we do this
    // to check if it is empty
    val isDiscoveredWordsEmpty by remember(words){ mutableStateOf(words.count() == 1 && words.contains("")) }
    val textToDisplay = if (isDiscoveredWordsEmpty){
        AnnotatedString(stringResource(R.string.puzzle_detail_word_list_empty))
    }else{
        buildAnnotatedString {
            words.reversed().forEachIndexed { index, word ->
                val fontWeight =
                    if (word in pangrams) FontWeight.ExtraBold else FontWeight.Normal
                withStyle(style = SpanStyle(fontWeight = fontWeight)) {
                    append(word.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.ENGLISH
                        ) else it.toString()
                    })
                }

                if (index < words.size - 1) {
                    append(" ")
                }
            }
        }
    }

    OutlinedButton(
        onClick = toggleWordBoxExpanded,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colors.onSurface,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {


            if (!expanded) {
                ChevronRow(textToDisplay.toString(), expanded = false)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChevronRow(
                        stringResource(
                            R.string.puzzle_detail_word_list_word_count,
                            if(isDiscoveredWordsEmpty) 0 else words.size.minus(1)
                        ), true
                    )
                    if (isDiscoveredWordsEmpty.not()) {
                        Spacer(Modifier.height(16.dp))
                        val modifiedList=words.minus("").toList()
                        ColumnGridList(modifiedList, pangrams)
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnGridList(words: List<String>, pangrams: Set<String>, columnNum: Int = 3) {
    val rows = words.sorted().chunked(columnNum)
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        items(rows) { rowWords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until columnNum) {
                    val word = rowWords.getOrNull(i) ?: ""
                    Text(
                        text = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() },
                        maxLines = 1,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (word in pangrams) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun GameRankingDialog(maxPuzzleScore: Int,
                      onDismiss: () -> Unit) {
    CustomDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.puzzle_ranking_dialog_title)
    ) {
        Text(
            text = stringResource(R.string.puzzle_ranking_description),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(16.dp))
        PuzzleRanking.sortedValues.minus(PuzzleRanking.QueenBee).forEach { ranking ->
            val rankName = stringResource(ranking.displayText)
            val rankScore = (maxPuzzleScore * (ranking.percentageCutOff / 100.0)).roundToInt()
            Text(text = "$rankName ($rankScore)")
        }
    }
}


@Composable
fun GameInformationDialog(onDismissGameInformationDialog:()->Unit){
        CustomDialog(
            onDismiss =onDismissGameInformationDialog,
            title = stringResource(R.string.puzzle_rules_dialog_title)
        ) {
            Text(
                text = stringResource(R.string.puzzle_rules_title),
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(16.dp))
            BulletedList(stringArrayResource(R.array.puzzle_rules))
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.puzzle_scoring_rules_title),
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(16.dp))
            BulletedList(stringArrayResource(R.array.puzzle_scoring_rules))
        }
}


@Composable
fun AnswerInputBox(modifier: Modifier=Modifier,centerLetter:Char, word:String){
    val textSize = 30.sp
    val highlightColor = MaterialTheme.colors.primary
    Row(modifier = modifier){
        Text(
            text = buildAnnotatedString {
                word.forEach { c ->
                    if (c == centerLetter) {
                        withStyle(style = SpanStyle(color = highlightColor)) {
                            append(c.uppercaseChar())
                        }
                    } else {
                        append(c.uppercaseChar())
                    }
                }
            },
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        val infiniteTransition = rememberInfiniteTransition()
        val cursorAnimation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        Text(
            text = "|",
            fontSize = textSize,
            color = highlightColor,
            fontWeight = FontWeight.Light,
            modifier = Modifier.alpha(if (cursorAnimation >= 0.5f) 1f else 0f)
        )
    }
}
@Composable
fun ChevronRow(
    text: String,
    expanded: Boolean,
    textColor: Color = Color.Black
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color = textColor
        )
        Spacer(modifier = Modifier.width(20.dp))
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null
        )
    }
}


@Composable
fun BulletedList(display:Array<String>){
    Column(modifier = Modifier.fillMaxWidth()) {
        display.forEach { string ->
            Row(verticalAlignment = Alignment.Top) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        modifier = Modifier.size(6.dp),
                        imageVector = Icons.Filled.Circle, contentDescription = "bullet point"
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = string,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
@Composable
fun PuzzleKeyboard(modifier: Modifier=Modifier,
                   requiredChar:Char,
                   onLetterPressed: (Char) -> Unit,
                   optionalLetters:List<Char>) {

    Layout(
        content = {
            CustomKeypad(letter = requiredChar, isCenter = true, onLetterPressed = onLetterPressed)
            optionalLetters.forEach {
                CustomKeypad(letter = it, isCenter = false, onLetterPressed = onLetterPressed)
            }
        }, modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map {
            val buttonSize = min(constraints.maxWidth, constraints.maxHeight) / 3
            it.measure(Constraints(maxWidth = buttonSize, maxHeight = buttonSize))
        }
        val width = placeables.first().measuredWidth
        val radius = (width / 2).toDouble()
        val centerToEdge = Math.sqrt(radius.pow(2.0) - (radius / 2.0).pow(2.0)).toInt()
        val height = centerToEdge * 2
        val gap = 30
        val offset = height + gap
        val totalWidth = ((offset * Math.cos(30.0 * (Math.PI / 180)) * 2) + width).toInt()
        val totalHeight = (height * 3) + (gap * 2)
        val centerX = totalWidth / 2 - width / 2
        val centerY = totalHeight / 2 - width / 2
        layout(totalWidth, totalHeight) {
            placeables.first().place(centerX, centerY)
            placeables.drop(1).forEachIndexed { index, placeable ->
                val angle = 30 + (index * 60)
                val x = centerX + Math.cos(angle * (Math.PI / 180)) * offset
                val y = centerY + Math.sin(angle * (Math.PI / 180)) * offset
                placeable.place(x.toInt(), y.toInt())
            }
        }
    }
}
@Composable
fun CustomKeypad(modifier: Modifier=Modifier,letter:Char,
                 isCenter:Boolean,onLetterPressed:(Char)->Unit){
    Button(modifier = modifier.fillMaxSize(),
        shape = hexagonalShape,
        colors = ButtonDefaults.buttonColors(if (isCenter) MaterialTheme.colors.primary
        else MaterialTheme.colors.secondary),
        onClick = {onLetterPressed(letter)},
        border = null){
        BoxWithConstraints(contentAlignment = Alignment.Center){
            val fontSize = with(LocalDensity.current){
                maxWidth.times(0.15f).toPx().sp}
            Text(letter.uppercaseChar().toString(), fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}
val hexagonalShape =  object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        /* use exterior angles to calculate the x,y of the lines to be drawn */
        val path = Path()
        val cx = size.width / 2
        val cy = size.height / 2
        var angle = 0
        for (i in 0 until 7) {
            angle += 60
            val x = kotlin.math.cos(angle * (Math.PI / 180)) * cx + cx
            val y = kotlin.math.sin(angle * (Math.PI / 180)) * cy + cy
            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        return Outline.Generic(path)
    }
}
@Composable
fun ResetGameConfirmationDialog(onDismissConfirmationDialog:()->Unit,
                                onResetGameConfirmed:()->Unit){
    AlertDialog(onDismissRequest = onDismissConfirmationDialog,
        title = { Text(stringResource(R.string.puzzle_detail_reset_confirm_title)) },
        text = { Text(stringResource(R.string.puzzle_detail_reset_confirm_body)) },
        confirmButton = {
            TextButton(onClick ={
                onDismissConfirmationDialog()
                onResetGameConfirmed()
            }){
                Text(stringResource(R.string.puzzle_detail_reset_confirm_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissConfirmationDialog){
                Text(stringResource(R.string.cancel))
            }
        })
}

@Composable
fun CustomDialog(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        fontSize = 24.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    content.invoke()
                }
            }
        }
    }
}

@Composable
fun WordToastRow(modifier: Modifier=Modifier,activeWordToast: WordToast?,
                 toastDurationMs:Long=1000,
                 dismissActiveWordToast:()->Unit){
    Row(modifier = modifier.fillMaxWidth().padding(10.dp)
        .graphicsLayer { alpha=if (activeWordToast != null) 1f else 0f
    }){
        val message = when (activeWordToast) {
            is WordToast.Success -> buildAnnotatedString {
                withStyle(SpanStyle(color = Color.Green)){
                    append("+${activeWordToast.pointValue}")
                }
            }
            is WordToast.Error -> {
                buildAnnotatedString {
                    withStyle(SpanStyle(color=Color.Red)){
                        append(stringResource(id = activeWordToast.wordError.errorMessage))
                    }
                }
            }
            null -> ""
        }

        Text(message.toString(),
            textAlign = TextAlign.Center)
    }
    LaunchedEffect(activeWordToast){
        delay(toastDurationMs)
        dismissActiveWordToast()
    }
}
@Composable
fun ActionBars(modifier:Modifier=Modifier,
               enterAnswerButtonClicked:()->Unit,
               deleteCharButtonClicked:()->Unit,
               shuffleCharsButtonClicked:()->Unit){
    Row(modifier =modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly){
        ActionButton(
            modifier = Modifier,
            onClick = deleteCharButtonClicked
        ) {
            Text(
                stringResource(R.string.puzzle_detail_actionbar_delete),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
        }
        ActionButton(
            modifier = Modifier,
            onClick = shuffleCharsButtonClicked,
            shape = CircleShape
        ) {
            Icon(
                Icons.Filled.Autorenew,
                stringResource(R.string.puzzle_detail_actionbar_shuffle)
            )
        }
        ActionButton(
            modifier = Modifier,
            onClick = enterAnswerButtonClicked
        ) {
            Text(
                stringResource(R.string.puzzle_detail_actionbar_enter),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(50),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colors.onSurface,
        ),
        shape = shape,
    ) {
        content()
    }
}