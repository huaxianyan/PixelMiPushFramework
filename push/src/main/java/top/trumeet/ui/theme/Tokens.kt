package top.trumeet.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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

/** Durations and easings shared by every animation, so transitions feel like one app. */
object Motion {
    /** A control the user just touched, a switch row for instance. */
    const val FeedbackMillis = 120

    /** Content appearing or disappearing inside a screen. */
    const val ContentMillis = 200

    /** A whole page entering or leaving. */
    const val PageMillis = 260

    /** Sideways travel of a pushed or popped page, as a fraction of the container width. */
    const val PageTravelFraction = 0.18f

    val EnterEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}
