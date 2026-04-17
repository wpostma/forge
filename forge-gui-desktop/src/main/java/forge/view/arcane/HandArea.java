/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane;

import java.awt.event.MouseEvent;

import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.MouseTriggerEvent;

/**
 * <p>
 * HandArea class.
 * </p>
 * 
 * @author Forge
 * @version $Id: HandArea.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class HandArea extends CardArea {
    /** Constant <code>serialVersionUID=7488132628637407745L</code>. */
    private static final long serialVersionUID = 7488132628637407745L;
    private static final String LAND_LIMIT_REJECTED_MESSAGE = "You have already played all lands allowed this turn.";
    private static final String PLAY_REJECTED_MESSAGE = "You can't do that right now.";

    /**
     * <p>
     * Constructor for HandArea.
     * </p>
     * TODO Make compatible with WindowBuilder
     * 
     * @param scrollPane
     */
    public HandArea(final CMatchUI matchUI, final FScrollPane scrollPane) {
        super(matchUI, scrollPane);

        this.setDragEnabled(true);
        this.setVertical(true);
        this.setMaxCardsPerRow(FModel.getPreferences().getPrefInt(FPref.UI_HAND_MAX_CARDS_PER_ROW));
        this.setNoOverlap(FModel.getPreferences().getPrefBoolean(FPref.UI_HAND_NO_OVERLAP));
    }

    @Override
    protected boolean cardPanelDraggable(final CardPanel panel) {
        return panel.getCard() != null;
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
        getMatchUI().setCard(panel.getCard(), evt.isShiftDown());
        super.mouseOver(panel, evt);
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        if (!getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt))) {
            showRejectedPlayFeedback(panel);
        }
        super.mouseLeftClicked(panel, evt);
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        if (!getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt))) {
            showRejectedPlayFeedback(panel);
        }
        super.mouseRightClicked(panel, evt);
    }

    private void showRejectedPlayFeedback(final CardPanel panel) {
        getMatchUI().flashIncorrectAction();
        final PlayerView controller = panel.getCard() == null ? null : panel.getCard().getController();
        getMatchUI().showPromptMessage(controller, getRejectedPlayMessage(panel, controller));
    }

    private String getRejectedPlayMessage(final CardPanel panel, final PlayerView controller) {
        if (panel.getCard() != null && panel.getCard().getCurrentState().isLand() && controller != null
                && !controller.hasUnlimitedLandPlay() && controller.getNumLandThisTurn() >= controller.getMaxLandPlay()) {
            return LAND_LIMIT_REJECTED_MESSAGE;
        }
        return PLAY_REJECTED_MESSAGE;
    }
}
