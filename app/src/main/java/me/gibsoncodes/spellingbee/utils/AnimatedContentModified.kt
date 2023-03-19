package me.gibsoncodes.spellingbee.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ModifiedAnimatedContent(modifier: Modifier =Modifier,
                            visible:Boolean,
                            enterAnimation: EnterTransition = slideInVertically(tween(800,
                                easing=LinearOutSlowInEasing)) { it/2 } + fadeIn(),
                            exitAnimation: ExitTransition = slideOutVertically(tween(800,easing= LinearOutSlowInEasing)) {
                                it/2 } + fadeOut(),
                            content:@Composable ()->Unit){
    AnimatedVisibility(visible = visible, modifier = modifier, enter =enterAnimation,exit=exitAnimation ){
        content()
    }
}