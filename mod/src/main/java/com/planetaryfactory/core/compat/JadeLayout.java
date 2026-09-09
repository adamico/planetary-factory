package com.planetaryfactory.core.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/**
 * How this pack's Jade tooltips put an icon and its figure on one line (#199, #209).
 *
 * <p>Two things are being fixed against Jade's default, and both are the renderer's rather than
 * ours: an item is drawn at 16 pixels against the font's 8, so an appended figure lands on the
 * icon's top edge rather than beside it; and appended elements butt straight up against each
 * other, which reads as one run of glyphs instead of a column of icons and a column of figures.
 *
 * <p>It lives here rather than in either plugin because the rig and the furnace have to agree --
 * a player reads the two machines side by side, and a spacer picked twice is a spacer that drifts.
 */
final class JadeLayout {

    private JadeLayout() {}

    /** Jade draws an item at 16 and the font at 8; the difference is what the figure is dropped by. */
    private static final float ICON_HEIGHT = 16F;
    private static final float TEXT_HEIGHT = 8F;

    /** The gap between an icon and the figure that belongs to it, in pixels. */
    private static final int GAP = 4;

    /** A figure dropped to the icon's centre line, with its leading gap already in place. */
    static void appendFigure(snownee.jade.api.ITooltip tooltip, IElementHelper elements, Component text) {
        tooltip.append(elements.spacer(GAP, 0));
        tooltip.append(elements.text(text).translate(new Vec2(0F, ICON_HEIGHT / 2F - TEXT_HEIGHT / 2F)));
    }

    /** An element opening a new line, gapped from whatever came before it on that line. */
    static void appendSpaced(snownee.jade.api.ITooltip tooltip, IElementHelper elements, IElement element) {
        tooltip.append(elements.spacer(GAP, 0));
        tooltip.append(element);
    }

    /** One icon-and-figure line: an icon, a gap, and the number that goes with it. */
    static void line(snownee.jade.api.ITooltip tooltip, IElementHelper elements, IElement icon, Component text) {
        tooltip.add(icon);
        appendFigure(tooltip, elements, text);
    }
}
