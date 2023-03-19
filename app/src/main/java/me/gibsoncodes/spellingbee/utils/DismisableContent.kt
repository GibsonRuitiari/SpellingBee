package me.gibsoncodes.spellingbee.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> DismissableComposable(modifier:Modifier=Modifier,item:T,
                              background: @Composable ()->Unit,
                              foreground:@Composable ()->Unit,
                              onDismissed:(()->Unit)?=null,
                              directions:Set<DismissDirection> = setOf(DismissDirection.StartToEnd)
){
    val dismissState = remember(item){ DismissState(DismissValue.Default) }
    val isDismissed = dismissState.isDismissed(DismissDirection.StartToEnd)
    if (isDismissed){
        LaunchedEffect(item){
            onDismissed?.invoke()
        }
    }
    AnimatedVisibility(modifier = modifier, visible = isDismissed.not(),
        exit=shrinkVertically(
        animationSpec = tween(
            durationMillis = 200,
        )
    ) + fadeOut()){
       SwipeToDismiss(modifier = Modifier, state = dismissState,
           directions = directions, background = {background()},
           dismissContent = {foreground()})
    }

}
