package io.github.marcel.loadstonetp.utils;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Centralized dialog and button creation utilities.
 * Eliminates duplicated patterns for dialog building.
 * Provides standard configurations and helpers for common dialog types.
 *
 * @since 1.0
 */
public final class DialogComponentBuilder {

    private static final int DEFAULT_BUTTON_WIDTH = 150;
    private static final int HEADER_BUTTON_WIDTH = 200;

    // Standard click options - reusable across all buttons
    private static final ClickCallback.Options DEFAULT_CLICK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build();

    private DialogComponentBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a back button that closes the current dialog.
     * Standard back button used in nearly every dialog.
     * @return Configured back button
     */
    public static ActionButton createBackButton() {
        return ActionButton.create(
                Component.text("← Back", NamedTextColor.GRAY),
                Component.text("Go back", NamedTextColor.GRAY),
                DEFAULT_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a close/exit button.
     * @return Configured close button
     */
    public static ActionButton createCloseButton() {
        return ActionButton.create(
                Component.text("✕ Close", NamedTextColor.RED),
                Component.text("Close this dialog", NamedTextColor.GRAY),
                DEFAULT_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a confirm button (accepts dialog state).
     * @return Configured confirm button
     */
    public static ActionButton createConfirmButton() {
        return ActionButton.create(
                Component.text("✓ Confirm", NamedTextColor.GREEN),
                Component.text("Confirm and continue", NamedTextColor.GRAY),
                DEFAULT_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a cancel button (rejects dialog state).
     * @return Configured cancel button
     */
    public static ActionButton createCancelButton() {
        return ActionButton.create(
                Component.text("✕ Cancel", NamedTextColor.RED),
                Component.text("Cancel this action", NamedTextColor.GRAY),
                DEFAULT_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a header button (non-clickable, just for display).
     * Used as section headers in dialogs.
     * @param label The header text
     * @return Configured header button
     */
    public static ActionButton createHeaderButton(String label) {
        Objects.requireNonNull(label, "Header label cannot be null");
        return ActionButton.create(
                Component.text(label, NamedTextColor.GOLD),
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a separator/spacer button.
     * Useful for visual organization in dialogs.
     * @return Empty spacer button
     */
    public static ActionButton createSpacer() {
        return ActionButton.create(
                Component.empty(),
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a standard error message button (non-clickable).
     * @param message The error message
     * @return Configured error display button
     */
    public static ActionButton createErrorDisplay(Component message) {
        Objects.requireNonNull(message, "Error message cannot be null");
        return ActionButton.create(
                Component.text("⚠ ", NamedTextColor.RED).append(message),
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a standard success message button (non-clickable).
     * @param message The success message
     * @return Configured success display button
     */
    public static ActionButton createSuccessDisplay(Component message) {
        Objects.requireNonNull(message, "Success message cannot be null");
        return ActionButton.create(
                Component.text("✓ ", NamedTextColor.GREEN).append(message),
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a standard info message button (non-clickable).
     * @param message The info message
     * @return Configured info display button
     */
    public static ActionButton createInfoDisplay(Component message) {
        Objects.requireNonNull(message, "Info message cannot be null");
        return ActionButton.create(
                Component.text("ⓘ ", NamedTextColor.AQUA).append(message),
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a standard text display button (non-clickable).
     * @param text The text to display
     * @return Configured text display button
     */
    public static ActionButton createTextDisplay(Component text) {
        Objects.requireNonNull(text, "Text cannot be null");
        return ActionButton.create(
                text,
                Component.empty(),
                HEADER_BUTTON_WIDTH,
                DialogAction.customClick((view, audience) -> {}, DEFAULT_CLICK_OPTIONS)
        );
    }

    /**
     * Creates a multi-action dialog with buttons arranged in specified columns.
     * @param title Dialog title
     * @param buttons List of action buttons
     * @param backButton Optional back button to add at the end
     * @param columns Number of columns to arrange buttons in (1-3)
     * @return Configured Dialog
     */
    public static Dialog createMultiActionDialog(
            Component title,
            List<ActionButton> buttons,
            ActionButton backButton,
            int columns) {
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(buttons, "Buttons list cannot be null");

        List<ActionButton> allButtons = new ArrayList<>(buttons);
        if (backButton != null) {
            allButtons.add(backButton);
        }

        final ActionButton finalBackButton = backButton;
        final int validColumns = Math.max(1, Math.min(3, columns));
        
        return Dialog.create(builder -> builder.empty()
                .type(DialogType.multiAction(allButtons, finalBackButton, validColumns))
        );
    }

    /**
     * Creates a two-column dialog (used frequently in admin panels).
     * @param title Dialog title
     * @param buttons List of action buttons (will be split into 2 columns)
     * @param backButton Optional back button
     * @return Configured Dialog
     */
    public static Dialog createTwoColumnDialog(
            Component title,
            List<ActionButton> buttons,
            ActionButton backButton) {
        return createMultiActionDialog(title, buttons, backButton, 2);
    }

    /**
     * Creates a three-column dialog (used in large admin panels).
     * @param title Dialog title
     * @param buttons List of action buttons
     * @param backButton Optional back button
     * @return Configured Dialog
     */
    public static Dialog createThreeColumnDialog(
            Component title,
            List<ActionButton> buttons,
            ActionButton backButton) {
        return createMultiActionDialog(title, buttons, backButton, 3);
    }

    /**
     * Gets the standard click options used throughout the plugin.
     * @return Standard ClickCallback.Options
     */
    public static ClickCallback.Options getDefaultClickOptions() {
        return DEFAULT_CLICK_OPTIONS;
    }
}
