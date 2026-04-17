package forge.gui.util;

import com.google.common.collect.ImmutableList;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;

import java.util.List;

public class SOptionPane {
    public static final FSkinProp QUESTION_ICON = FSkinProp.ICO_QUESTION;
    public static final FSkinProp INFORMATION_ICON = FSkinProp.ICO_INFORMATION;
    public static final FSkinProp WARNING_ICON = FSkinProp.ICO_WARNING;
    public static final FSkinProp ERROR_ICON = FSkinProp.ICO_ERROR;

    public static void showMessageDialog(final String message, final String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(final String message) {
        showMessageDialog(message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(final String message, final String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(final String message, final String title, final FSkinProp icon) {
        logDialogEvent("showMessageDialog", title, message, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")), 0);
        showOptionDialog(message, title, icon, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")), 0);
    }

    public static boolean showConfirmDialog(final String message) {
        return showConfirmDialog(message, "Forge");
    }

    public static boolean showConfirmDialog(final String message, final String title) {
        return showConfirmDialog(message, title, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final boolean defaultYes) {
        return showConfirmDialog(message, title, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), defaultYes);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, defaultYes, false);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes, final boolean noicon) {
        final List<String> options = ImmutableList.of(yesButtonText, noButtonText);
        final int reply = SOptionPane.showOptionDialog(message, title, noicon ? null : QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options) {
        return showOptionDialog(message, title, icon, options, 0);
    }

    public static int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        logDialogEvent("showOptionDialog", title, message, options, defaultOption);
        return GuiBase.getInterface().showOptionDialog(message, title, icon, options, defaultOption);
    }

    public static String showInputDialog(final String message, final String title) {
        return showInputDialog(message, title, null, "", null, false);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon) {
        return showInputDialog(message, title, icon, "", null, false);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null, false);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, boolean isNumeric) {
        logDialogEvent("showInputDialog", title, message, inputOptions, -1);
        return GuiBase.getInterface().showInputDialog(message, title, icon, initialInput, inputOptions, isNumeric);
    }

    private static void logDialogEvent(final String method, final String title, final String message,
            final List<?> options, final int defaultOption) {
        if (!ForgePreferences.DEV_MODE) {
            return;
        }

        // Desktop dialogs already log at FOptionPane layer; avoid duplicate lines there.
        if (GuiBase.getInterface() != null && !GuiBase.getInterface().isLibgdxPort()) {
            return;
        }

        final String safeTitle = sanitizeForLog(title);
        final String safeMessage = sanitizeForLog(message);
        final String safeOptions = options == null ? "null" : sanitizeForLog(options.toString());
        System.out.println("[DIALOG] method=" + method
                + ", title=" + safeTitle
                + ", message=" + safeMessage
                + ", options=" + safeOptions
                + ", defaultOption=" + defaultOption);
    }

    private static String sanitizeForLog(final String text) {
        if (text == null) {
            return "<null>";
        }
        String sanitized = text.replace('\r', ' ').replace('\n', ' ');
        if (sanitized.length() > 260) {
            sanitized = sanitized.substring(0, 260) + "...";
        }
        return sanitized;
    }

    private SOptionPane() {
    }
}
