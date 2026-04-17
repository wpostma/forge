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
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

import forge.game.card.CardView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.controllers.CPrompt;
import forge.toolbox.FButton;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of message report.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VPrompt implements IVDoc<CPrompt> {
    private static final Color ATTENTION_BORDER_COLOR = new Color(255, 198, 35);
    private static final Color ATTENTION_BORDER_PULSE_COLOR = new Color(255, 238, 121);
    private static final Color ATTENTION_BUTTON_COLOR = new Color(255, 224, 88);
    private static final Color ATTENTION_BUTTON_PULSE_COLOR = Color.WHITE;
    private static final int PROMPT_FONT_SIZE = 24;
    private static final int COMPACT_PROMPT_FONT_SIZE = 20;
    private static final int PROMPT_HEADER_FONT_SIZE = 18;
    private static final int PROMPT_BUTTON_FONT_SIZE = 22;
    private static final int ATTENTION_PULSE_DELAY_MS = 1400;
    private static final int ANNOUNCEMENT_FLASH_DELAY_MS = 130;
    private static final int ANNOUNCEMENT_FLASH_TICKS = 10;
    private static final float PROMPT_TEXT_ZOOM = 4.0f;
    private static final float PROMPT_TEXT_MIN_ZOOM = 2.0f;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblPrompt"));

    // Various components
    private final FButton btnOK = new FButton(localizer.getMessage("lblOK"));
    private final FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
    private final FHtmlViewer tarMessage = new FHtmlViewer();
    private final FScrollPane messageScroller = new FScrollPane(tarMessage, false,
    		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JLabel lblGames;
    private CardView card = null ; 
    private JPanel container;
    private FButton attentionButton;
    private boolean needsAttention;
    private boolean attentionPulseBright;
    private int announcementFlashTicks;
    private final Timer attentionPulseTimer = new Timer(ATTENTION_PULSE_DELAY_MS, e -> toggleAttentionPulse());
    private final Timer announcementFlashTimer = new Timer(ANNOUNCEMENT_FLASH_DELAY_MS, e -> flashAnnouncementBorder());
    private final ComponentAdapter fitPromptTextToViewport = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            fitPromptText();
        }
    };

    public void setCardView(final CardView card) {
	this.card = card ;
    }

    private KeyAdapter buttonKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (btnCancel.isEnabled()) {
                    if (FModel.getPreferences().getPrefBoolean(FPref.UI_ALLOW_ESC_TO_END_TURN) || !btnCancel.getText().equals("End Turn")) {
                        btnCancel.doClick();
                    }
                }
            }
        }
    };

    private final CPrompt controller;

    //========= Constructor
    public VPrompt(final CPrompt controller) {
        this.controller = controller;

        lblGames = new FLabel.Builder()
        .fontSize(PROMPT_HEADER_FONT_SIZE)
        .fontStyle(Font.PLAIN)
        .fontAlign(SwingConstants.CENTER)
        .opaque()
        .build();

        btnOK.addKeyListener(buttonKeyAdapter);
        btnCancel.addKeyListener(buttonKeyAdapter);
        btnOK.setFont(FSkin.getBoldFont(PROMPT_BUTTON_FONT_SIZE));
        btnCancel.setFont(FSkin.getBoldFont(PROMPT_BUTTON_FONT_SIZE));
        attentionPulseTimer.setInitialDelay(0);
        announcementFlashTimer.setInitialDelay(0);

        tarMessage.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarMessage.setMargin(new Insets(8, 8, 8, 8));
        tarMessage.setZoomFactors(PROMPT_TEXT_ZOOM, PROMPT_TEXT_MIN_ZOOM);
        tarMessage.getAccessibleContext().setAccessibleName("Prompt");
        tarMessage.setFocusable(true); // Allow tab to navigate to the prompt.
        messageScroller.getViewport().addComponentListener(fitPromptTextToViewport);
        messageScroller.getViewport().getView().addMouseListener(new MouseAdapter() {
        	@Override 
        	public void mouseEntered(final MouseEvent e) {
        		if ( card != null ) {
			    controller.getMatchUI().setCard(card);
        		}
        	}
        });
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
    	ForgePreferences prefs = FModel.getPreferences();
        container = parentCell.getBody();

        // wrap   : 2 columns required for btnOk and btnCancel.
        container.setLayout(new MigLayout("wrap 2, gap 4px!, insets 3px 3px 5px 3px"));
        updateAttentionBorder();
        if (prefs.getPrefBoolean(FPref.UI_COMPACT_PROMPT)) { //hide header and use smaller font if compact prompt
            tarMessage.setFont(FSkin.getRelativeFont(COMPACT_PROMPT_FONT_SIZE));
        }
        else {
        	container.add(lblGames, "span 2, w 10:100%, h 36px!");
            tarMessage.setFont(FSkin.getRelativeFont(PROMPT_FONT_SIZE));
        }
        lblGames.setText(localizer.getMessage("lblGameSetup"));

        container.add(messageScroller, "span 2, w 10:100%, h 0:100%");

        boolean largerButtons = prefs.getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String constraints = largerButtons ? "w 10:50%, h 74px!" : "w 10:50%, h 58px!";
        constraints += ", gaptop 2px!";

        container.add(btnOK, constraints);
        container.add(btnCancel, constraints);
        fitPromptText();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_MESSAGE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CPrompt getLayoutControl() {
        return controller;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    //========= Retrieval methods
    /** @return {@link javax.swing.JButton} */
    public FButton getBtnOK() {
        return this.btnOK;
    }

    /** @return {@link javax.swing.JButton} */
    public FButton getBtnCancel() {
        return this.btnCancel;
    }

    /** @return {@link javax.swing.JTextArea} */
    public FHtmlViewer getTarMessage() {
        return this.tarMessage;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblGames() {
        return this.lblGames;
    }

    public void announceNewQuestion() {
        announcementFlashTicks = ANNOUNCEMENT_FLASH_TICKS;
        updateAttentionBorder();
        SwingUtilities.invokeLater(this::fitPromptText);
        if (!announcementFlashTimer.isRunning()) {
            announcementFlashTimer.start();
        }
    }

    public void setNeedsAttention(final boolean needsAttention0, final FButton defaultButton) {
        if (attentionButton != null && attentionButton != defaultButton) {
            attentionButton.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }

        needsAttention = needsAttention0;
        attentionButton = needsAttention ? defaultButton : null;

        if (needsAttention) {
            attentionPulseBright = true;
            updateAttentionPulse();
            if (!attentionPulseTimer.isRunning()) {
                attentionPulseTimer.start();
            }
        } else {
            attentionPulseTimer.stop();
            attentionPulseBright = false;
            updateAttentionPulse();
        }
    }

    private void toggleAttentionPulse() {
        attentionPulseBright = !attentionPulseBright;
        updateAttentionPulse();
    }

    private void flashAnnouncementBorder() {
        announcementFlashTicks--;
        updateAttentionBorder();
        if (announcementFlashTicks <= 0) {
            announcementFlashTimer.stop();
            updateAttentionBorder();
        }
    }

    private void updateAttentionPulse() {
        updateAttentionBorder();
        btnOK.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        btnCancel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        if (needsAttention && attentionButton != null && attentionButton.isEnabled()) {
            attentionButton.setForeground(attentionPulseBright ? ATTENTION_BUTTON_PULSE_COLOR : ATTENTION_BUTTON_COLOR);
            attentionButton.repaint();
        }
    }

    private void updateAttentionBorder() {
        if (container == null) {
            return;
        }

        final Color borderColor;
        if (announcementFlashTicks > 0) {
            borderColor = announcementFlashTicks % 2 == 0 ? ATTENTION_BUTTON_PULSE_COLOR : ATTENTION_BORDER_PULSE_COLOR;
        } else if (needsAttention) {
            borderColor = attentionPulseBright ? ATTENTION_BORDER_PULSE_COLOR : ATTENTION_BORDER_COLOR;
        } else {
            borderColor = FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor();
        }
        final Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, announcementFlashTicks > 0 || needsAttention ? 3 : 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2));
        container.setBorder(border);
        container.repaint();
    }

    private void fitPromptText() {
        tarMessage.fitZoomTo(messageScroller.getViewport().getExtentSize());
    }
}
