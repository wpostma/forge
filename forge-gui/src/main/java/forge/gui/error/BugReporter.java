/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.error;

import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;
import io.sentry.Sentry;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The class ErrorViewer. Enables showing and saving error messages that
 * occurred in forge.
 *
 * @author Clemens Koza
 * @version V1.0 02.08.2009
 */
public class BugReporter {
    private static final int STACK_OVERFLOW_MAX_MESSAGE_LEN = 16 * 1024;
    private static final String DEFAULT_REPORT = "Report";
    private static final String DEFAULT_SAVE = "Save";
    private static final String DEFAULT_DISCARD = "Discard";
    private static final String DEFAULT_EXIT = "Exit";
    private static final String DEFAULT_SENTRY = "Automatically submit bug reports";
    private static final String DEFAULT_REPORT_CRASH = "Report a Crash";
    private static final String DEFAULT_REPORT_BUG = "Report a Bug";
    private static final String DEFAULT_SAVE_ERROR_MESSAGE = "There was an error while saving: {0}";
    private static final String DEFAULT_SAVE_ERROR_TITLE = "Error Saving File";

    private static Throwable exception;
    private static String message;


    /**
     * Shows exception information in a format ready to post to the forum as a
     * crash report. Uses the exception's message as the reason if message is
     * null.
     */
    public static void reportException(final Throwable ex, final String message) {
        if (ex == null) {
            return;
        }
        exception = ex;
        if (message != null) {
            System.err.printf("%s > %s%n", FThreads.debugGetCurrThreadId(), message);
        }
        System.err.print(FThreads.debugGetCurrThreadId() + " > ");
        ex.printStackTrace();

        final StringBuilder sb = new StringBuilder();
        if (null != message && !message.isEmpty()) {
            Sentry.addBreadcrumb(message);
            sb.append(FThreads.debugGetCurrThreadId()).append(" > ").append(message).append("\n");
        }

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        final String swStr = sw.toString();
        if (ex instanceof StackOverflowError && swStr.length() >= STACK_OVERFLOW_MAX_MESSAGE_LEN) {
            // most likely a cycle.  only take first portion so the message
            // doesn't grow too large to post
            sb.append(swStr, 0, STACK_OVERFLOW_MAX_MESSAGE_LEN);
            sb.append("\n... (truncated)");
        }
        else {
            sb.append(swStr);
        }
        System.err.println("BugReporter: prepared crash report text (" + sb.length() + " chars).");
        if (isSentryEnabled()) {
            System.err.println("BugReporter: Sentry is enabled, sending report.");
            sendSentry();
        } else {
            System.err.println("BugReporter: Sentry disabled, attempting to show bug report dialog.");
            showBugReportDialog(getReportCrashLabel(), sb.toString(), true);
        }
    }

    public static boolean isSentryEnabled() {
        return FModel.getPreferences() != null && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.USE_SENTRY);
    }
    /**
     * Alias for reportException(ex, null).
     */
    public static void reportException(final Throwable ex) {
        reportException(ex, null);
    }

    /**
     * Alias for reportException(ex, String.format(format, args)).
     */
    public static void reportException(final Throwable ex, final String format, final Object... args) {
        reportException(ex, String.format(format, args));
    }

    /**
     * Shows a forum post template for reporting a bug.
     */
    public static void reportBug(final String details) {
        final StringBuilder sb = new StringBuilder();
        if (null != details && !details.isEmpty()) {
            sb.append("\n\n");
            sb.append(details);
        }
        message = sb.toString();

        if (isSentryEnabled()) {
            sendSentry();
        } else {
            showBugReportDialog(getReportBugLabel(), message, false);
        }
    }

    public static void saveToFile(final String error) {
        File f;
        String text;
        if (GuiBase.getInterface().isLibgdxPort()) {
            text = GuiBase.getHWInfo() + "\n\n" + error;
            // Save in downloads directory instead for easy access without filepicker
            String filename = "forge-bug-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".txt";
            f = new File(GuiBase.getDownloadsDir() + filename);
        } else {
            text = error;
            final long curTime = System.currentTimeMillis();
            for (int i = 0; ; i++) {
                final String name = String.format("%TF-%02d.txt", curTime, i);
                f = new File(name);
                if (!f.exists()) {
                    break;
                }
            }

            f = GuiBase.getInterface().getSaveFile(f);
        }

        if (f == null) {
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))){
            bw.write(text);
        } catch (final IOException ex) {
            SOptionPane.showMessageDialog(getLocalized("lblThereErrorWasDuringSaving", DEFAULT_SAVE_ERROR_MESSAGE, ex),
            getLocalized("lblErrorSavingFile", DEFAULT_SAVE_ERROR_TITLE), SOptionPane.ERROR_ICON);
        }
    }

    public static void sendSentry() {
        try {
            if (exception != null) {
                Sentry.captureException(exception);
            } else if (message !=null) {
                Sentry.captureMessage(message);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private BugReporter() {
    }

    public static String getReportLabel() {
        return getLocalized("lblReport", DEFAULT_REPORT);
    }

    public static String getSaveLabel() {
        return getLocalized("lblSave", DEFAULT_SAVE);
    }

    public static String getDiscardLabel() {
        return getLocalized("lblDiscardError", DEFAULT_DISCARD);
    }

    public static String getExitLabel() {
        return getLocalized("lblExit", DEFAULT_EXIT);
    }

    public static String getSentryLabel() {
        return getLocalized("lblAutoSubmitBugReports", DEFAULT_SENTRY);
    }

    public static String getReportCrashLabel() {
        return getLocalized("lblReportCrash", DEFAULT_REPORT_CRASH);
    }

    public static String getReportBugLabel() {
        return getLocalized("btnReportBug", DEFAULT_REPORT_BUG);
    }

    private static String getLocalized(final String key, final String defaultValue, final Object... messageArguments) {
        try {
            return Localizer.getInstance().getMessageorUseDefault(key, defaultValue, messageArguments);
        }
        catch (final Exception ex) {
            return defaultValue;
        }
    }

    private static void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        try {
            if (GuiBase.getInterface() != null) {
                System.err.println("BugReporter: forwarding bug report dialog to GUI interface " + GuiBase.getInterface().getClass().getName());
                GuiBase.getInterface().showBugReportDialog(title, text, showExitAppBtn);
                System.err.println("BugReporter: GUI interface returned from showBugReportDialog.");
                return;
            }
            System.err.println("BugReporter: no GUI interface available, falling back to stderr.");
        }
        catch (final Throwable dialogFailure) {
            System.err.println("Failed to display bug report dialog.");
            dialogFailure.printStackTrace();
        }

        System.err.println("BugReporter: stderr fallback follows.");
        System.err.println(title);
        System.err.println(text);
    }
}
