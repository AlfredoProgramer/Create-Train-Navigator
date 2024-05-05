package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;

public class ExpandButton extends DLButton {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;
    
    // Data
    private boolean expanded;

    private static final String expandText = "▼ " + TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.expand").getString();
    private static final String collapseText = "▲ " + TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.collapse").getString();
    

    public ExpandButton(int pX, int pY, boolean initialState, Consumer<ExpandButton> onClick) {
        super(pX, pY, 20, 20, TextUtils.empty(), onClick);
        this.expanded = initialState;
        
        int w1 = font.width(expandText) + 10;
        int w2 = font.width(collapseText) + 10;
        int h = font.lineHeight + 6;

        width = w1 > w2 ? w1 : w2;
        height = h;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (isMouseOver(pMouseX, pMouseY)) {
            expanded = !expanded;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (isMouseOver(pMouseX, pMouseY)) {
            GuiUtils.fill(graphics, getX(), getY(), width, height, 0x1AFFFFFF);
            GuiUtils.drawString(graphics, font, getX() + width / 2, getY() + height / 2 - font.lineHeight / 2, expanded ? TextUtils.text(collapseText).withStyle(ChatFormatting.UNDERLINE) : TextUtils.text(expandText).withStyle(ChatFormatting.UNDERLINE), 0xFFFFFF, EAlignment.CENTER, true);
        } else {            
            GuiUtils.drawString(graphics, font, getX() + width / 2, getY() + height / 2 - font.lineHeight / 2, expanded ? TextUtils.text(collapseText) : TextUtils.text(expandText), 0xFFFFFF, EAlignment.CENTER, true);
        }        
    }

    public boolean isExpanded() {
        return expanded;
    }
    
}
