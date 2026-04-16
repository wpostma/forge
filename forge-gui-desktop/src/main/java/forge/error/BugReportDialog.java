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
package forge.error;

import java.awt.Frame;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.WrapLayout;
import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import net.miginfocom.swing.MigLayout;

/**
 * The class BugReportDialog. Enables showing and saving error messages that
 * occurred in Forge.
 * 
 * @author Clemens Koza
 */
public class BugReportDialog {
    private static boolean dialogShown;

    public static void show(String title, String text, boolean showExitAppBtn) {
        System.err.println("BugReportDialog: show invoked on thread \"" + Thread.currentThread().getName() + "\"");
        if (!SwingUtilities.isEventDispatchThread()) {
            System.err.println("BugReportDialog: not on EDT, invoking and waiting.");
            try {
                SwingUtilities.invokeAndWait(() -> show(title, text, showExitAppBtn));
            }
            catch (final Exception e) {
                System.err.println("BugReportDialog: invokeAndWait failed.");
                throw new RuntimeException(e);
            }
            return;
        }

        System.err.println("BugReportDialog: running on EDT.");
        if (dialogShown) {
            System.err.println("BugReportDialog: dialog already shown, skipping.");
            return;
        }

        if (Singletons.getView() != null && Singletons.getView().getSplash() != null) {
            System.err.println("BugReportDialog: disposing splash before showing dialog.");
            Singletons.getView().getSplash().dispose();
        }
        else {
            System.err.println("BugReportDialog: no splash to dispose.");
        }

        JTextArea area = new JTextArea(text);
        area.setFont(new Font("Monospaced", Font.PLAIN, 10));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JPanel helpPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 2));
        JPanel p = new JPanel(new MigLayout("wrap"));
        p.add(helpPanel, "w 600");
        p.add(new JScrollPane(area), "w 100%, h 100%, gaptop 5");

        // Button is not modified, String gets the automatic listener to hide
        // the dialog
        List<Object> options = new ArrayList<>();
        options.add(new JButton(new _Report()));
        // option to enable automatic Sentry submission
        options.add(new JCheckBox(new _ActivateSentry()));
        options.add(new JLabel(BugReporter.getSentryLabel()));
        options.add(new JButton(new _SaveAction(area)));
        options.add(BugReporter.getDiscardLabel());
        if (showExitAppBtn) {
            options.add(new JButton(new _ExitAction()));
        }

        JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options.toArray(), options.get(0));
        final Frame owner = Singletons.getView() == null ? null : Singletons.getView().getFrame();
        System.err.println("BugReportDialog: owner is " + (owner == null ? "null" : owner.getClass().getName() + ", displayable=" + owner.isDisplayable()));
        JDialog dlg = pane.createDialog(owner != null && owner.isDisplayable() ? owner : null, title);
        dlg.setSize(showExitAppBtn ? 780 : 600, 400);
        dlg.setResizable(true);
        dlg.setAlwaysOnTop(true);
        dialogShown = true;
        System.err.println("BugReportDialog: about to show modal dialog.");
        dlg.setVisible(true);
        System.err.println("BugReportDialog: modal dialog closed.");
        dlg.dispose();
        dialogShown = false;
    }

    @SuppressWarnings("serial")
    private static class _ActivateSentry extends AbstractAction {

        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            JCheckBox checkBox = (JCheckBox)actionEvent.getSource();
            // enable Sentry use in the future through preference setting
            FModel.getPreferences().setPref(ForgePreferences.FPref.USE_SENTRY, checkBox.isSelected());
            FModel.getPreferences().save();
        }
    }

    @SuppressWarnings("serial")
    private static class _Report extends AbstractAction {

        public _Report() {
            super(BugReporter.getReportLabel());
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            BugReporter.sendSentry();
            JOptionPane.getRootFrame().dispose();
        }
    }

    @SuppressWarnings("serial")
    private static class _SaveAction extends AbstractAction {
        private final JTextArea area;

        public _SaveAction(final JTextArea areaParam) {
            super(BugReporter.getSaveLabel());
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.area = areaParam;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            BugReporter.saveToFile(area.getText());
        }
    }

    @SuppressWarnings("serial")
    private static class _ExitAction extends AbstractAction {
        public _ExitAction() {
            super(BugReporter.getExitLabel());
            this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    }
    
    // disable instantiation
    private BugReportDialog() { }
}
