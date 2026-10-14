package top.trumeet.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
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

/** Spatial page motion. NavHost seeks the same pop transitions during predictive back. */
object Motion {
    private const val BackgroundTravelFraction = 1f / 3f
    private val Spatial = spring<IntOffset>()

    // Same-level tab effects never participate in a seekable hierarchy return.
    val TabEnter = fadeIn()
    val TabExit = fadeOut()

    val Enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, Spatial)
    }

    val Exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            Spatial,
            targetOffset = { (it * BackgroundTravelFraction).toInt() }
        )
    }

    val PopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            Spatial,
            initialOffset = { (it * BackgroundTravelFraction).toInt() }
        )
    }

    val PopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, Spatial)
    }
}
