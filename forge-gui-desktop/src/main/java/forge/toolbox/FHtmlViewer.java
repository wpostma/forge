package forge.toolbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import forge.toolbox.FSkin.SkinnedEditorPane;

/**
 * Viewer for HTML
 *
 */
@SuppressWarnings("serial")
public class FHtmlViewer extends SkinnedEditorPane {
    private static final Pattern HTML_IMAGE_TAG = Pattern.compile("(?i)<img\\b[^>]*>");
    private static final Pattern IMAGE_DIMENSION_ATTRIBUTE = Pattern.compile(
            "(?i)\\b(width|height)\\s*=\\s*([\"']?)(\\d+)([\"']?)");

    private float preferredZoomFactor = 1f;
    private float minimumZoomFactor = 1f;
    private float activeZoomFactor = 1f;
    private String currentHtmlText = "";

    /** */
    public FHtmlViewer() {
        super();
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setFocusable(false);
        this.setEditable(false);
        this.setContentType("text/html");
        this.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        updateHtmlFontRule();
    }

    /** @param str {@java.lang.String} */
    public FHtmlViewer(final String str) {
        this();
        this.setText(str);
    }

    @Override
    public void setText(final String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            setTextNow(text);
        } else {
            SwingUtilities.invokeLater(() -> setTextNow(text));
        }
    }

    private void setTextNow(final String text) {
        currentHtmlText = null == text ? "" : text.replaceAll("(\r\n)|(\n)", "<br>");
        activeZoomFactor = preferredZoomFactor;
        updateHtmlFontRule();
        setSuperText(getRenderedHtmlText());
        setCaretPosition(0); //keep scrolled to top
    }

    @Override
    public void setFont(final Font font) {
        super.setFont(font);
        updateHtmlFontRule();
    }

    @Override
    public void setForeground(final Color color) {
        super.setForeground(color);
        updateHtmlFontRule();
    }

    private void setSuperText(final String text) {
        super.setText(text);
    }

    public void setZoomFactor(final float zoomFactor) {
        setZoomFactors(zoomFactor, zoomFactor);
    }

    public void setZoomFactors(final float preferredZoomFactor0, final float minimumZoomFactor0) {
        preferredZoomFactor = Math.max(0.1f, preferredZoomFactor0);
        minimumZoomFactor = Math.max(0.1f, Math.min(preferredZoomFactor, minimumZoomFactor0));
        activeZoomFactor = preferredZoomFactor;
        updateHtmlFontRule();
        if (!currentHtmlText.isEmpty()) {
            setSuperText(getRenderedHtmlText());
        }
        revalidate();
        repaint();
    }

    public float getActiveZoomFactor() {
        return activeZoomFactor;
    }

    public void fitZoomTo(final Dimension extentSize) {
        if (extentSize == null || extentSize.width <= 0 || extentSize.height <= 0 || currentHtmlText.isEmpty()) {
            return;
        }

        final float[] candidateZooms = {
                preferredZoomFactor,
                minimumZoomFactor,
                1.5f,
                1.0f,
                0.75f
        };

        float appliedZoom = preferredZoomFactor;
        float lastTriedZoom = -1f;
        for (final float candidateZoom : candidateZooms) {
            if (candidateZoom <= 0f || candidateZoom > preferredZoomFactor || Math.abs(candidateZoom - lastTriedZoom) < 0.001f) {
                continue;
            }

            lastTriedZoom = candidateZoom;
            activeZoomFactor = candidateZoom;
            updateHtmlFontRule();
            setSuperText(getRenderedHtmlText());

            final Dimension preferredSize = getPreferredSize();
            appliedZoom = candidateZoom;
            if (preferredSize.width <= extentSize.width && preferredSize.height <= extentSize.height) {
                break;
            }
        }

        activeZoomFactor = appliedZoom;
        updateHtmlFontRule();
        setSuperText(getRenderedHtmlText());

        setCaretPosition(0);
        revalidate();
        repaint();
    }

    private void updateHtmlFontRule() {
        if (!(getEditorKit() instanceof HTMLEditorKit) || getFont() == null || getForeground() == null) {
            return;
        }

        final Font font = getFont();
        final String fontFamily = font.getFamily().replace("\\", "\\\\").replace("'", "\\'");
        final String fontWeight = font.isBold() ? "bold" : "normal";
        final int fontSize = Math.max(1, Math.round(font.getSize() * activeZoomFactor));
        final String color = String.format("#%02x%02x%02x",
                getForeground().getRed(), getForeground().getGreen(), getForeground().getBlue());
        final String rule = String.format(
                "body, div, p, span { font-family: '%s'; font-size: %dpt; font-weight: %s; color: %s; margin: 0; }",
                fontFamily, fontSize, fontWeight, color);
        final StyleSheet styleSheet = ((HTMLEditorKit) getEditorKit()).getStyleSheet();
        styleSheet.addRule(rule);
        final Document document = getDocument();
        if (document instanceof HTMLDocument) {
            ((HTMLDocument) document).getStyleSheet().addRule(rule);
        }
    }

    private String getRenderedHtmlText() {
        if (activeZoomFactor == 1f || !currentHtmlText.contains("<img")) {
            return currentHtmlText;
        }

        final Matcher imageMatcher = HTML_IMAGE_TAG.matcher(currentHtmlText);
        final StringBuffer htmlBuffer = new StringBuffer();
        while (imageMatcher.find()) {
            imageMatcher.appendReplacement(htmlBuffer, Matcher.quoteReplacement(scaleImageTag(imageMatcher.group())));
        }
        imageMatcher.appendTail(htmlBuffer);
        return htmlBuffer.toString();
    }

    private String scaleImageTag(final String imageTag) {
        final Matcher dimensionMatcher = IMAGE_DIMENSION_ATTRIBUTE.matcher(imageTag);
        final StringBuffer tagBuffer = new StringBuffer();
        while (dimensionMatcher.find()) {
            final int value = Integer.parseInt(dimensionMatcher.group(3));
            final int scaledValue = Math.max(1, Math.round(value * activeZoomFactor));
            dimensionMatcher.appendReplacement(tagBuffer, Matcher.quoteReplacement(
                    dimensionMatcher.group(1) + "=" + dimensionMatcher.group(2) + scaledValue + dimensionMatcher.group(4)));
        }
        dimensionMatcher.appendTail(tagBuffer);
        return tagBuffer.toString();
    }
}
