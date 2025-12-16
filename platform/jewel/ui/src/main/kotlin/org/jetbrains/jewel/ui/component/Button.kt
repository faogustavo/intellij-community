@file:Suppress("KDocUnresolvedReference")

package org.jetbrains.jewel.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.Stroke
import org.jetbrains.jewel.foundation.modifier.border
import org.jetbrains.jewel.foundation.modifier.thenIf
import org.jetbrains.jewel.foundation.state.CommonStateBitMask.Active
import org.jetbrains.jewel.foundation.state.CommonStateBitMask.Enabled
import org.jetbrains.jewel.foundation.state.CommonStateBitMask.Focused
import org.jetbrains.jewel.foundation.state.CommonStateBitMask.Hovered
import org.jetbrains.jewel.foundation.state.CommonStateBitMask.Pressed
import org.jetbrains.jewel.foundation.state.FocusableComponentState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.foundation.theme.LocalTextStyle
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.component.styling.MenuStyle
import org.jetbrains.jewel.ui.component.styling.SplitButtonStyle
import org.jetbrains.jewel.ui.disabledAppearance
import org.jetbrains.jewel.ui.focusOutline
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Stroke as PainterHintStroke
import org.jetbrains.jewel.ui.theme.defaultButtonStyle
import org.jetbrains.jewel.ui.theme.defaultSplitButtonStyle
import org.jetbrains.jewel.ui.theme.menuStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle
import org.jetbrains.jewel.ui.theme.outlinedSplitButtonStyle

/**
 * A button that follows the default visual styling with customizable content and behavior.
 *
 * Provides a clickable component that follows the standard button interactions including hover, press, and focus
 * states. The button adapts its appearance based on the enabled/disabled state and supports custom styling.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:** [`JButton`](https://docs.oracle.com/javase/tutorial/uiswing/components/button.html)
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button. Use this to observe state changes or customize interaction handling
 * @param style The visual styling configuration for the button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param content The content to be displayed inside the button
 * @see javax.swing.JButton
 */
@Composable
public fun DefaultButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: ButtonStyle = JewelTheme.defaultButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    content: @Composable () -> Unit,
) {
    ButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        content = content,
    )
}

/**
 * A button with an outlined visual style and customizable appearance.
 *
 * Similar to [DefaultButton] but with a different visual treatment that emphasizes the button boundary. Useful for
 * secondary actions or when you want to reduce visual weight.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:** [`JButton`](https://docs.oracle.com/javase/tutorial/uiswing/components/button.html)
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button. Use this to observe state changes or customize interaction handling
 * @param style The visual styling configuration for the button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param content The content to be displayed inside the button
 * @see javax.swing.JButton
 */
@Composable
public fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: ButtonStyle = JewelTheme.outlinedButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    content: @Composable () -> Unit,
) {
    ButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using an outlined visual style.
 *
 * Similar to [DefaultSplitButton] but with an outlined visual treatment. Provides two interactive areas: the main
 * button area for the primary action and a chevron section that opens a dropdown menu.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param menuContent The content builder for defining menu items in the dropdown
 * @param modifier Modifier to be applied to the button
 * @param popupModifier Modifier to be applied to the dropdown menu container
 * @param maxPopupHeight The maximum height of the popup
 * @param maxPopupWidth The maximum width of the popup. If it's unspecified, it will allow the content to grow as needed
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param content The content to be displayed in the main button area
 * @see com.intellij.ui.components.JBOptionButton
 */
@Composable
public fun OutlinedSplitButton(
    onClick: () -> Unit,
    menuContent: MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    popupModifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    maxPopupWidth: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.outlinedSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    secondaryOnClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = false,
        modifier = modifier,
        popupModifier = popupModifier,
        maxPopupHeight = maxPopupHeight,
        maxPopupWidth = maxPopupWidth,
        secondaryContentMenu = menuContent,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using an outlined visual style.
 *
 * Similar to [DefaultSplitButton] but with an outlined visual treatment. Provides two interactive areas: the main
 * button area for the primary action and a chevron section that opens a dropdown menu.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param content The content to be displayed in the main button area
 * @param menuContent The content builder for defining menu items in the dropdown
 * @see com.intellij.ui.components.JBOptionButton
 */
@Suppress("ComposableParamOrder", "ContentTrailingLambda") // To fix in JEWEL-925
@Composable
@Deprecated(
    "Deprecated in favor of the method with 'popupModifier', 'maxPopupHeight, and 'maxPopupWidth' parameters",
    level = DeprecationLevel.HIDDEN,
)
public fun OutlinedSplitButton(
    onClick: () -> Unit,
    secondaryOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.outlinedSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    content: @Composable () -> Unit,
    menuContent: MenuScope.() -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = false,
        modifier = modifier,
        secondaryContentMenu = menuContent,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using an outlined visual style.
 *
 * Similar to [DefaultSplitButton] but with an outlined visual treatment. Provides two interactive areas: the main
 * button area for the primary action and a chevron section that opens a dropdown menu.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param modifier Modifier to be applied to the button
 * @param popupModifier Modifier to be applied to the dropdown menu container
 * @param maxPopupHeight The maximum height of the popup
 * @param maxPopupWidth The maximum width of the popup. If it's unspecified, it will allow the content to grow as needed
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param content The content to be displayed in the main button area
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param popupContainer A generic container for the popup content
 * @see com.intellij.ui.components.JBOptionButton
 */
@Composable
public fun OutlinedSplitButton(
    onClick: () -> Unit,
    popupContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    popupModifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    maxPopupWidth: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.outlinedSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    secondaryOnClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = false,
        modifier = modifier,
        popupModifier = popupModifier,
        maxPopupHeight = maxPopupHeight,
        maxPopupWidth = maxPopupWidth,
        secondaryContent = popupContainer,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using an outlined visual style.
 *
 * Similar to [DefaultSplitButton] but with an outlined visual treatment. Provides two interactive areas: the main
 * button area for the primary action and a chevron section that opens a dropdown menu.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param content The content to be displayed in the main button area
 * @param popupContainer A generic container for the popup content
 * @see com.intellij.ui.components.JBOptionButton
 */
@Suppress("ComposableParamOrder", "ContentTrailingLambda") // To fix in JEWEL-925
@Composable
@Deprecated(
    "Deprecated in favor of the method with 'popupModifier', 'maxPopupHeight, and 'maxPopupWidth' parameters",
    level = DeprecationLevel.HIDDEN,
)
public fun OutlinedSplitButton(
    onClick: () -> Unit,
    secondaryOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.outlinedSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    content: @Composable () -> Unit,
    popupContainer: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = false,
        modifier = modifier,
        secondaryContent = popupContainer,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using the default visual style.
 *
 * Provides two interactive areas: the main button area for the primary action and a chevron section that opens a
 * dropdown menu with additional options.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param menuContent The content builder for defining menu items in the dropdown
 * @param modifier Modifier to be applied to the button
 * @param popupModifier Modifier to be applied to the dropdown menu container
 * @param maxPopupHeight The maximum height of the popup
 * @param maxPopupWidth The maximum width of the popup. If it's unspecified, it will allow the content to grow as needed
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param content The content to be displayed in the main button area
 * @see com.intellij.ui.components.JBOptionButton
 */
@Composable
public fun DefaultSplitButton(
    onClick: () -> Unit,
    menuContent: MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    popupModifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    maxPopupWidth: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.defaultSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    secondaryOnClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = true,
        modifier = modifier,
        popupModifier = popupModifier,
        maxPopupHeight = maxPopupHeight,
        maxPopupWidth = maxPopupWidth,
        secondaryContentMenu = menuContent,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using the default visual style.
 *
 * Provides two interactive areas: the main button area for the primary action and a chevron section that opens a
 * dropdown menu with additional options.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param content The content to be displayed in the main button area
 * @param menuContent The content builder for defining menu items in the dropdown
 * @see com.intellij.ui.components.JBOptionButton
 */
@Suppress("ComposableParamOrder", "ContentTrailingLambda") // To fix in JEWEL-925
@Composable
@Deprecated(
    "Deprecated in favor of the method with 'popupModifier', 'maxPopupHeight, and 'maxPopupWidth' parameters",
    level = DeprecationLevel.HIDDEN,
)
public fun DefaultSplitButton(
    onClick: () -> Unit,
    secondaryOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.defaultSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    content: @Composable () -> Unit,
    menuContent: MenuScope.() -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = true,
        modifier = modifier,
        secondaryContentMenu = menuContent,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using the default visual style.
 *
 * Provides two interactive areas: the main button area for the primary action and a chevron section that opens a
 * dropdown menu with additional options.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param popupContainer A generic container for the popup content
 * @param modifier Modifier to be applied to the button
 * @param popupModifier Modifier to be applied to the dropdown menu container
 * @param maxPopupHeight The maximum height of the popup
 * @param maxPopupWidth The maximum width of the popup. If it's unspecified, it will allow the content to grow as needed
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param content The content to be displayed in the main button area
 * @see com.intellij.ui.components.JBOptionButton
 */
@Suppress("ComposableParamOrder", "ContentTrailingLambda") // To fix in JEWEL-925
@Composable
public fun DefaultSplitButton(
    onClick: () -> Unit,
    popupContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    popupModifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    maxPopupWidth: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.defaultSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    secondaryOnClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = true,
        modifier = modifier,
        popupModifier = popupModifier,
        maxPopupHeight = maxPopupHeight,
        maxPopupWidth = maxPopupWidth,
        secondaryContent = popupContainer,
        content = content,
    )
}

/**
 * A split button combining a primary action with a dropdown menu, using the default visual style.
 *
 * Provides two interactive areas: the main button area for the primary action and a chevron section that opens a
 * dropdown menu with additional options.
 *
 * **Guidelines:** [on IJP SDK webhelp](https://plugins.jetbrains.com/docs/intellij/split-button.html)
 *
 * **Usage example:**
 * [`Buttons.kt`](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/samples/standalone/src/main/kotlin/org/jetbrains/jewel/samples/standalone/view/component/Buttons.kt)
 *
 * **Swing equivalent:**
 * [`JBOptionButton`](https://github.com/JetBrains/intellij-community/tree/idea/243.22562.145/platform/platform-api/src/com/intellij/ui/components/JBOptionButton.kt)
 *
 * @param onClick Will be called when the user clicks the main button area
 * @param secondaryOnClick Will be called when the user clicks the dropdown/chevron section
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When false, the button will not be clickable
 * @param interactionSource An optional [MutableInteractionSource] for observing and emitting [Interaction]s for this
 *   button
 * @param style The visual styling configuration for the split button including colors, metrics and layout parameters
 * @param textStyle The typography style to be applied to the button's text content
 * @param menuStyle The visual styling configuration for the dropdown menu
 * @param content The content to be displayed in the main button area
 * @param popupContainer A generic container for the popup content
 * @see com.intellij.ui.components.JBOptionButton
 */
@Suppress("ComposableParamOrder", "ContentTrailingLambda") // To fix in JEWEL-925
@Composable
@Deprecated(
    "Deprecated in favor of the method with 'popupModifier', 'maxPopupHeight, and 'maxPopupWidth' parameters",
    level = DeprecationLevel.HIDDEN,
)
public fun DefaultSplitButton(
    onClick: () -> Unit,
    secondaryOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: SplitButtonStyle = JewelTheme.defaultSplitButtonStyle,
    textStyle: TextStyle = JewelTheme.defaultTextStyle,
    menuStyle: MenuStyle = JewelTheme.menuStyle,
    content: @Composable () -> Unit,
    popupContainer: @Composable () -> Unit,
) {
    SplitButtonImpl(
        onClick = onClick,
        secondaryOnClick = secondaryOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        style = style,
        textStyle = textStyle,
        menuStyle = menuStyle,
        isDefault = true,
        modifier = modifier,
        secondaryContent = popupContainer,
        content = content,
    )
}

@Composable
private fun SplitButtonImpl(
    onClick: () -> Unit,
    secondaryOnClick: () -> Unit,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    style: SplitButtonStyle,
    textStyle: TextStyle,
    menuStyle: MenuStyle,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
    popupModifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    maxPopupWidth: Dp = Dp.Unspecified,
    secondaryContent: @Composable (() -> Unit)? = null,
    secondaryContentMenu: (MenuScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var popupVisible by remember { mutableStateOf(false) }

    var buttonWidth by remember { mutableStateOf(Dp.Unspecified) }
    var buttonState by remember(interactionSource) { mutableStateOf(ButtonState.of(enabled = enabled)) }
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.semantics { isTraversalGroup = true }) {
        @Suppress("ModifierNotUsedAtRoot") // This is the "true" root, the box is only for the popup
        ButtonImpl(
            onClick = { if (enabled) onClick() },
            modifier =
                modifier
                    .onSizeChanged { buttonWidth = with(density) { it.width.toDp() } }
                    .onFocusChanged {
                        if (!it.isFocused) {
                            popupVisible = false
                        }
                    }
                    .thenIf(enabled) {
                        onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when {
                                keyEvent.key == Key.DirectionDown -> {
                                    popupVisible = true
                                    true
                                }

                                else -> false
                            }
                        }
                    },
            enabled = enabled,
            interactionSource = interactionSource,
            style = style.button,
            textStyle = textStyle,
            content = content,
            secondaryContent = {
                Divider(
                    orientation = Orientation.Vertical,
                    thickness = style.metrics.dividerMetrics.thickness,
                    modifier =
                        Modifier.heightIn(min = style.button.metrics.minSize.height)
                            .fillMaxHeight()
                            .padding(vertical = style.metrics.dividerPadding)
                            .align(Alignment.CenterStart),
                    color = if (enabled) style.colors.dividerColor else style.colors.dividerDisabledColor,
                )
                
                SplitButtonChevron(
                    style = style,
                    enabled = enabled,
                    isDefault = isDefault,
                    forceFocused = popupVisible,
                    onChevronClick = {
                        secondaryOnClick()
                        popupVisible = !popupVisible
                        if (!buttonState.isFocused) focusRequester.requestFocus()
                    },
                    onStateChange = { state -> buttonState = state },
                    modifier =
                        Modifier.testTag("Jewel.SplitButton.SecondaryAction")
                            .fillMaxHeight()
                            .focusRequester(focusRequester),
                )
            },
        )

        if (popupVisible && enabled) {
            val splitButtonPopupModifier =
                Modifier.heightIn(max = maxPopupHeight)
                    .widthIn(min = buttonWidth, max = maxPopupWidth.coerceAtLeast(buttonWidth))
                    .testTag("Jewel.SplitButton.Popup")
                    .then(popupModifier)
                    .onClick { popupVisible = false }

            when {
                secondaryContentMenu != null -> {
                    PopupMenu(
                        modifier = splitButtonPopupModifier,
                        onDismissRequest = {
                            popupVisible = false
                            true
                        },
                        horizontalAlignment = Alignment.Start,
                        style = menuStyle,
                        content = secondaryContentMenu,
                    )
                }
                secondaryContent != null -> {
                    PopupContainer(
                        modifier = splitButtonPopupModifier,
                        onDismissRequest = { popupVisible = false },
                        horizontalAlignment = Alignment.Start,
                        content = secondaryContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitButtonChevron(
    style: SplitButtonStyle,
    enabled: Boolean,
    isDefault: Boolean,
    forceFocused: Boolean,
    onChevronClick: () -> Unit,
    onStateChange: (ButtonState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var buttonState by remember { mutableStateOf(ButtonState.of(enabled = enabled, focused = forceFocused)) }
    val interactionSource = remember { MutableInteractionSource() }
    // This helps with managing and keeping the button focus state in sync
    // when the Composable is used for the SplitButton variant.
    // This variant is the only one that overrides the default focus state
    // according to the popup visibility.
    var actuallyFocused by remember { mutableStateOf(false) }

    LaunchedEffect(buttonState, onStateChange) { onStateChange(buttonState) }
    LaunchedEffect(enabled) { buttonState = buttonState.copy(enabled = enabled) }
    LaunchedEffect(forceFocused) {
        buttonState = buttonState.copy(focused = if (forceFocused) true else actuallyFocused)
    }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            buttonState =
                when (interaction) {
                    is FocusInteraction -> {
                        val isFocused = interaction is FocusInteraction.Focus
                        actuallyFocused = isFocused
                        buttonState.copy(focused = isFocused)
                    }
                    is HoverInteraction -> buttonState.copy(hovered = interaction is HoverInteraction.Enter)
                    is PressInteraction -> buttonState.copy(pressed = interaction is PressInteraction.Press)
                    else -> buttonState
                }
        }
    }

    val shape =
        RoundedCornerShape(
            topStart = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
            topEnd = style.button.metrics.cornerSize,
            bottomEnd = style.button.metrics.cornerSize,
        )
    val outerBorderWidth = if (isDefault) 0.dp else style.button.metrics.borderWidth

    Box(
        modifier
            .fillMaxHeight()
            .padding(top = outerBorderWidth, end = outerBorderWidth, bottom = outerBorderWidth)
            .width(style.button.metrics.minSize.height)
            .heightIn(min = style.button.metrics.minSize.height)
            .clickable(
                enabled = enabled,
                onClick = onChevronClick,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
            )
            .focusOutline(
                state = buttonState,
                outlineShape = shape,
                alignment = if (isDefault) Stroke.Alignment.Outside else Stroke.Alignment.Inside,
                expand =
                    if (isDefault) {
                        style.button.metrics.focusOutlineExpand
                    } else {
                        0.dp
                    },
            )
    ) {
        Icon(
            key = AllIconsKeys.General.ChevronDown,
            contentDescription = "Chevron",
            modifier = Modifier.align(Alignment.Center).thenIf(!enabled) { disabledAppearance() },
            hints =
                if (isDefault && enabled) {
                    arrayOf(PainterHintStroke(style.colors.chevronColor))
                } else {
                    emptyArray()
                },
        )
    }
}

@Composable
private fun ButtonImpl(
    onClick: () -> Unit,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    style: ButtonStyle,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    secondaryContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var buttonState by remember(interactionSource) { mutableStateOf(ButtonState.of(enabled = enabled)) }

    remember(enabled) { buttonState = buttonState.copy(enabled = enabled) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            buttonState =
                when (interaction) {
                    is PressInteraction.Press -> buttonState.copy(pressed = true)
                    is PressInteraction.Cancel,
                    is PressInteraction.Release -> buttonState.copy(pressed = false)

                    is HoverInteraction.Enter -> buttonState.copy(hovered = true)
                    is HoverInteraction.Exit -> buttonState.copy(hovered = false)
                    is FocusInteraction -> buttonState.copy(focused = interaction is FocusInteraction.Focus)

                    else -> buttonState
                }
        }
    }

    val shape = RoundedCornerShape(style.metrics.cornerSize)
    val colors = style.colors
    val borderColor by colors.borderFor(buttonState)
    val backgroundColor by colors.backgroundFor(buttonState)

    Box(
        modifier =
            modifier
                .background(backgroundColor, shape)
                .border(Stroke.Alignment.Inside, style.metrics.borderWidth, borderColor, shape)
                .semantics { isTraversalGroup = true },
        propagateMinConstraints = true,
    ) {
        val contentColor by colors.contentFor(buttonState)

        CompositionLocalProvider(
            LocalContentColor provides contentColor.takeOrElse { textStyle.color },
            LocalTextStyle provides textStyle.copy(color = contentColor.takeOrElse { textStyle.color }),
        ) {
            Row(
                modifier =
                    Modifier.semantics { isTraversalGroup = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.clickable(
                            onClick = onClick,
                            enabled = enabled,
                            role = Role.Button,
                            interactionSource = interactionSource,
                            indication = null,
                        )
                        .focusOutline(
                            state = buttonState,
                            outlineShape = shape,
                            alignment = style.focusOutlineAlignment,
                            expand = style.metrics.focusOutlineExpand,
                        )
                        .defaultMinSize(style.metrics.minSize.width)
                        .height(style.metrics.minSize.height)
                        .padding(style.metrics.padding)
                        .thenIf(secondaryContent != null) { weight(1f, fill = false) }
                ) {
                    content()
                }
                secondaryContent?.invoke()
            }
        }
    }
}

@Immutable
@JvmInline
public value class ButtonState(public val state: ULong) : FocusableComponentState {
    override val isActive: Boolean
        get() = state and Active != 0UL

    override val isEnabled: Boolean
        get() = state and Enabled != 0UL

    override val isFocused: Boolean
        get() = state and Focused != 0UL

    override val isHovered: Boolean
        get() = state and Hovered != 0UL

    override val isPressed: Boolean
        get() = state and Pressed != 0UL

    public fun copy(
        enabled: Boolean = isEnabled,
        focused: Boolean = isFocused,
        pressed: Boolean = isPressed,
        hovered: Boolean = isHovered,
        active: Boolean = isActive,
    ): ButtonState = of(enabled = enabled, focused = focused, pressed = pressed, hovered = hovered, active = active)

    override fun toString(): String =
        "${javaClass.simpleName}(isEnabled=$isEnabled, isFocused=$isFocused, isHovered=$isHovered, " +
            "isPressed=$isPressed, isActive=$isActive)"

    public companion object {
        public fun of(
            enabled: Boolean = true,
            focused: Boolean = false,
            pressed: Boolean = false,
            hovered: Boolean = false,
            active: Boolean = true,
        ): ButtonState =
            ButtonState(
                (if (enabled) Enabled else 0UL) or
                    (if (focused) Focused else 0UL) or
                    (if (hovered) Hovered else 0UL) or
                    (if (pressed) Pressed else 0UL) or
                    (if (active) Active else 0UL)
            )
    }
}
