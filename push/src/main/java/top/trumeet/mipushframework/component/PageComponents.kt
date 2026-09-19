package top.trumeet.mipushframework.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import top.trumeet.ui.theme.Layout

private const val DisabledAlpha = 0.38f

/**
 * A page's content column.
 *
 * Every screen draws through this one: the same padding at the edges, the same gap between
 * cards, and a column that stops growing on wide screens instead of stretching a line of text
 * across a tablet.
 */
@Composable
fun PageColumn(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Layout.CardGap),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = modifier
                .widthIn(max = Layout.ContentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = Layout.PageHorizontal,
                    vertical = Layout.PageVertical
                ),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * The title above a card.
 *
 * It sits on the page background rather than inside the card, which is what keeps a long
 * settings screen readable: the card holds the controls, the title only names the group.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier
            .padding(start = 8.dp, top = Layout.LineGap, bottom = Layout.LineGap)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * One group of rows: an optional heading on top of a card.
 *
 * The card itself carries the grouping, so the rows inside it must not draw their own
 * background, and no divider is needed between them.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * One row inside a [SettingsCard].
 *
 * [onClick] decides whether the row is interactive at all: a row that only shows a value passes
 * none, and then it carries no ripple either.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent),
    onClick: (() -> Unit)? = null
) {
    val clickable = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(enabled = enabled, onClick = onClick)
    }
    ListItem(
        modifier = clickable.alpha(if (enabled) 1f else DisabledAlpha),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (summary.isNullOrEmpty()) {
            null
        } else {
            {
                // No explicit colour: ListItem paints this with colors.supportingColor, so a
                // caller that overrides the row colours keeps control of it.
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        leadingContent = leading,
        trailingContent = trailing,
        colors = colors
    )
}

/** The chevron that marks a row opening another page. */
@Composable
fun NavigationChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A row that opens another page, marked with the chevron the platform uses for it. */
@Composable
fun NavigationRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        summary = summary,
        leading = leading,
        enabled = enabled,
        onClick = onClick,
        trailing = { NavigationChevron() }
    )
}

/** The coloured dot that stands for a state, always the same size and always round. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = Layout.StatusDot) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * A supporting line that holds its place even when it has nothing to say.
 *
 * A card whose second line is missing comes out shorter than the cards around it, and a column
 * of uneven cards reads as broken. This reserves an empty line of exactly the height a written
 * one would take, so an application that never received a push lines up with the rest.
 */
@Composable
fun SupportingLine(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium
    val lineHeight = with(LocalDensity.current) {
        if (style.lineHeight.isSpecified) style.lineHeight.toDp() else 20.dp
    }

    Box(modifier = modifier.heightIn(min = lineHeight)) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** What a list shows when it has nothing to show. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
