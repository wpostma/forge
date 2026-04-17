package forge.test.toolbox;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import forge.GuiDesktop;
import forge.localinstance.properties.ForgeConstants;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FSkin;

/**
 * Manual harness for checking FHtmlViewer sizing outside the match UI.
 */
public final class FHtmlViewerZoomHarness {
    private static final int EXPECTED_SYMBOL_WIDTH = FSkin.getEncodedSymbolImageWidth();
    private static final int EXPECTED_SYMBOL_HEIGHT = FSkin.getEncodedSymbolImageHeight();

    private FHtmlViewerZoomHarness() {
    }

    public static void main(final String[] args) {
        initializeForgePreferences();
        if (args.length > 0 && "--snapshot".equals(args[0])) {
            writeSnapshot();
            return;
        }
        EventQueue.invokeLater(FHtmlViewerZoomHarness::show);
    }

    private static void initializeForgePreferences() {
        if (GuiBase.getInterface() == null) {
            GuiBase.setInterface(new GuiDesktop());
        }
        if (FModel.getPreferences() != null) {
            return;
        }

        final ForgePreferences preferences = GuiBase.getForgePrefs();
        try {
            final Field preferencesField = FModel.class.getDeclaredField("preferences");
            preferencesField.setAccessible(true);
            preferencesField.set(null, preferences);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to initialize Forge preferences for GUI harness", ex);
        }

        ForgePreferences.DEV_MODE = preferences.getPrefBoolean(ForgePreferences.FPref.DEV_MODE_ENABLED);
        ForgePreferences.UPLOAD_DRAFT = ForgePreferences.NET_CONN;
    }

    private static void writeSnapshot() {
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    final BufferedImage image = new BufferedImage(1200, 620, BufferedImage.TYPE_INT_ARGB);
                    final Graphics2D g = image.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, image.getWidth(), image.getHeight());
                    paintSample(g, "1x", 1.0f, 30, new Color(245, 250, 255));
                    paintSample(g, "2x", 2.0f, 420, new Color(255, 250, 230));
                    paintSample(g, "4x", 4.0f, 810, new Color(245, 255, 240));
                    g.dispose();

                    final File output = new File("forge-gui-test/target/fhtmlviewer-zoom-harness.png");
                    output.getParentFile().mkdirs();
                    ImageIO.write(image, "png", output);
                    System.out.println(output.getAbsolutePath());
                } catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void paintSample(final Graphics2D g, final String label, final float zoom, final int x,
            final Color background) {
        final FHtmlViewer viewer = new FHtmlViewer();
        viewer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        viewer.setForeground(Color.BLACK);
        viewer.setOpaque(true);
        viewer.setBackground(background);
        viewer.setZoomFactor(zoom);
        viewer.setText("Priority: WarpFactor\nTurn 2 (WarpFactor)\nPhase: Main phase, precombat\nStack: Empty");
        viewer.setSize(340, 470);
        viewer.fitZoomTo(new Dimension(340, 470));
        viewer.validate();

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(label + " active=" + viewer.getActiveZoomFactor()
                + " pref=" + viewer.getPreferredSize().width + "x" + viewer.getPreferredSize().height, x, 18);
        g.setColor(Color.GRAY);
        g.drawRect(x - 1, 29, 342, 472);
        g.translate(x, 30);
        viewer.paint(g);
        g.translate(-x, -30);
    }

    private static void show() {
        final JFrame frame = new JFrame("Forge UI Tests Tool");
        final FHtmlViewer viewer = new FHtmlViewer();
        final JScrollPane scroller = new JScrollPane(viewer);
        final JLabel status = new JLabel();

        viewer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        viewer.setZoomFactors(4.0f, 2.0f);

        scroller.getViewport().addChangeListener(e -> {
            viewer.fitZoomTo(scroller.getViewport().getExtentSize());
            updateStatus(status, viewer, scroller.getViewport().getExtentSize());
        });

        final JPanel cacheButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addCacheButton(cacheButtons, "Generate symbols", status, () -> {
            FSkin.rebuildEncodingSymbolsCache();
            return validateSymbolCache();
        });
        addCacheButton(cacheButtons, "Validate symbols", status, FHtmlViewerZoomHarness::validateSymbolCache);
        addCacheButton(cacheButtons, "Purge symbols", status, () -> {
            purgeSymbolCache();
            return validateSymbolCache();
        });
        addCacheButton(cacheButtons, "Open symbols folder", status, () -> {
            openSymbolsFolder();
            return validateSymbolCache();
        });

        final JPanel contentButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        addContentButton(contentButtons, "Short prompt", viewer, status, scroller,
                "Do you want to play or draw?");
        addContentButton(contentButtons, "Priority", viewer, status, scroller,
                "Priority: WarpFactor\nTurn: 2 (WarpFactor)\nPhase: Main phase, precombat\nStack: Empty");
        addContentButton(contentButtons, "Long choice", viewer, status, scroller,
                "Choose one:\n\n"
                + "- Return target permanent to its owner's hand.\n"
                + "- Draw two cards, then discard a card.\n"
                + "- Create a tapped Treasure token.");
        addContentButton(contentButtons, "Raw HTML", viewer, status, scroller,
                "<html><b>Bold question</b><br>Pay Mana Cost: {W}{R}{R}</html>");

        final JPanel buttons = new JPanel(new GridLayout(2, 1));
        buttons.add(cacheButtons);
        buttons.add(contentButtons);

        final JPanel top = new JPanel(new BorderLayout());
        top.add(buttons, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);
        frame.add(scroller, BorderLayout.CENTER);
        frame.setSize(new Dimension(980, 480));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        runStartupCacheCheck(status);
        viewer.setText("Do you want to play or draw?");
        EventQueue.invokeLater(() -> {
            viewer.fitZoomTo(scroller.getViewport().getExtentSize());
            updateStatus(status, viewer, scroller.getViewport().getExtentSize());
            appendCacheStatus(status, validateSymbolCache());
        });
    }

    private static void runStartupCacheCheck(final JLabel status) {
        final String cacheStatus = validateSymbolCache();
        status.setText("startup " + cacheStatus);
    }

    private static void addCacheButton(final JPanel buttons, final String label, final JLabel status,
            final CacheAction action) {
        final JButton button = new JButton(label);
        button.addActionListener(e -> {
            try {
                final String cacheStatus = action.run();
                status.setText(cacheStatus);
            } catch (final RuntimeException ex) {
                status.setText("cache: ERROR - " + ex.getMessage());
            }
        });
        buttons.add(button);
    }

    private static void addContentButton(final JPanel buttons, final String label, final FHtmlViewer viewer,
            final JLabel status, final JScrollPane scroller, final String content) {
        final JButton button = new JButton(label);
        button.addActionListener(e -> {
            viewer.setText(FSkin.encodeSymbols(content, false));
            EventQueue.invokeLater(() -> {
                viewer.fitZoomTo(scroller.getViewport().getExtentSize());
                updateStatus(status, viewer, scroller.getViewport().getExtentSize());
            });
        });
        buttons.add(button);
    }

    private static void updateStatus(final JLabel status, final FHtmlViewer viewer, final Dimension extent) {
        status.setText(String.format("font=%s %d, zoom=%.1fx, preferred=%dx%d, viewport=%dx%d",
                viewer.getFont().getFamily(),
                viewer.getFont().getSize(),
                viewer.getActiveZoomFactor(),
                viewer.getPreferredSize().width,
                viewer.getPreferredSize().height,
                extent.width,
                extent.height));
    }

    private static void appendCacheStatus(final JLabel status, final String cacheStatus) {
        status.setText(status.getText() + " | " + cacheStatus);
    }

    private static String validateSymbolCache() {
        final File symbolsDir = new File(ForgeConstants.CACHE_SYMBOLS_DIR);
        if (!symbolsDir.exists()) {
            return "cache: MISSING dir " + symbolsDir.getAbsolutePath();
        }

        final File[] symbolFiles = symbolsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (symbolFiles == null || symbolFiles.length == 0) {
            return "cache: EMPTY dir " + symbolsDir.getAbsolutePath();
        }

        int validCount = 0;
        final List<String> invalid = new ArrayList<>();
        for (final File symbolFile : symbolFiles) {
            try {
                final BufferedImage image = ImageIO.read(symbolFile);
                if (image == null) {
                    invalid.add(symbolFile.getName() + " unreadable");
                    continue;
                }
                if (image.getWidth() == EXPECTED_SYMBOL_WIDTH && image.getHeight() == EXPECTED_SYMBOL_HEIGHT) {
                    validCount++;
                } else {
                    invalid.add(symbolFile.getName() + " " + image.getWidth() + "x" + image.getHeight());
                }
            } catch (final IOException ex) {
                invalid.add(symbolFile.getName() + " " + ex.getClass().getSimpleName());
            }
        }

        if (invalid.isEmpty()) {
            return String.format("cache: OK %d png files at %s", validCount, symbolsDir.getAbsolutePath());
        }

        return String.format("cache: FAIL %d/%d valid (%s expected), bad=%s",
                validCount,
                symbolFiles.length,
                EXPECTED_SYMBOL_WIDTH + "x" + EXPECTED_SYMBOL_HEIGHT,
                invalid.get(0));
    }

    private static void purgeSymbolCache() {
        final File symbolsDir = new File(ForgeConstants.CACHE_SYMBOLS_DIR);
        if (!symbolsDir.exists()) {
            return;
        }

        final File[] symbolFiles = symbolsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (symbolFiles == null) {
            return;
        }

        for (final File symbolFile : symbolFiles) {
            if (!symbolFile.delete()) {
                throw new IllegalStateException("Could not delete " + symbolFile.getAbsolutePath());
            }
        }
    }

    private static void openSymbolsFolder() {
        final File symbolsDir = new File(ForgeConstants.CACHE_SYMBOLS_DIR);
        symbolsDir.mkdirs();
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Desktop API is not supported on this platform");
        }
        try {
            Desktop.getDesktop().open(symbolsDir);
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not open " + symbolsDir.getAbsolutePath(), ex);
        }
    }

    @FunctionalInterface
    private interface CacheAction {
        String run();
    }
}
