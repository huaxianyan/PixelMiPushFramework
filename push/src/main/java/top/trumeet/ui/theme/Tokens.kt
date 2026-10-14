package top.trumeet.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.unit.dp

/**
 * The layout rhythm every screen shares.
 *
 * Values are kept in step with the sibling apps (SyncClipboard, SevenMirror, WalletAssistant):
 * one page padding, one gap between cards, one padding inside them, and a content column that
 * stops growing and gets centred on wide screens.
 */
object Layout {
    /** Horizontal padding between the page edge and its content. */
    val PageHorizontal = 20.dp

    /** Vertical padding at the top and the bottom of a page's content. */
    val PageVertical = 12.dp

    /** Vertical gap between two cards of the same page. */
    val CardGap = 12.dp

    /** Gap between two rows of a lazy list. */
    val ListGap = 8.dp

    /** Padding on all four sides inside a card. */
    val CardPadding = 20.dp

    /** Vertical gap between two lines inside a card. */
    val LineGap = 8.dp

    /** A page's content stops growing past this width and is centred instead. */
    val ContentMaxWidth = 720.dp

    /** Long press target side for an application icon. */
    val AppIcon = 40.dp

    /** Diameter of a status dot. */
    val StatusDot = 12.dp
}

/**
 * Page motion aligned with Pixel Text's AppNavigation.kt (a505089d75b3, Apache-2.0).
 * Uses this app's logical directions and NavHost; predictive Back seeks the same pop transitions.
 */
object Motion {
    private const val ForegroundDurationMillis = 300
    private const val BackgroundDurationMillis = 260
    private const val FadeDurationMillis = 90
    private const val BackgroundFadeDelayMillis = 120
    private const val ForegroundFadeDelayMillis = 210
    private const val BackgroundTravelFraction = 1f / 4f
    private const val ForegroundEntryScale = 0.96f
    private const val ForegroundExitScale = 0.90f
    private const val BackgroundScale = 0.92f

    private val ForegroundSpatial = tween<IntOffset>(
        durationMillis = ForegroundDurationMillis, easing = FastOutSlowInEasing
    )
    private val BackgroundSpatial = tween<IntOffset>(
        durationMillis = BackgroundDurationMillis, easing = FastOutSlowInEasing
    )
    private val ForegroundScaling = tween<Float>(
        durationMillis = ForegroundDurationMillis, easing = FastOutSlowInEasing
    )
    private val BackgroundScaling = tween<Float>(
        durationMillis = BackgroundDurationMillis, easing = FastOutSlowInEasing
    )
    private val EnterFade = tween<Float>(durationMillis = FadeDurationMillis)
    private val BackgroundExitFade = tween<Float>(
        durationMillis = FadeDurationMillis, delayMillis = BackgroundFadeDelayMillis
    )
    private val ForegroundExitFade = tween<Float>(
        durationMillis = FadeDurationMillis, delayMillis = ForegroundFadeDelayMillis
    )

    // Same-level tab effects never participate in a seekable hierarchy return.
    val TabEnter = fadeIn()
    val TabExit = fadeOut()

    val Enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, ForegroundSpatial) +
            fadeIn(EnterFade) +
            scaleIn(initialScale = ForegroundEntryScale, animationSpec = ForegroundScaling)
    }

    val Exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            BackgroundSpatial,
            targetOffset = { (it * BackgroundTravelFraction).toInt() }
        ) + fadeOut(BackgroundExitFade) +
            scaleOut(targetScale = BackgroundScale, animationSpec = BackgroundScaling)
    }

    val PopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            BackgroundSpatial,
            initialOffset = { (it * BackgroundTravelFraction).toInt() }
        ) + fadeIn(EnterFade) +
            scaleIn(initialScale = BackgroundScale, animationSpec = BackgroundScaling)
    }

    val PopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, ForegroundSpatial) +
            fadeOut(ForegroundExitFade) +
            scaleOut(targetScale = ForegroundExitScale, animationSpec = ForegroundScaling)
    }
}
