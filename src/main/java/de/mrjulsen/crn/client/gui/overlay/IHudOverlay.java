package de.mrjulsen.crn.client.gui.overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public interface IHudOverlay {
    int getId();
    void render(ForgeGui gui, GuiGraphics graphics, int width, int height, float partialTicks);
    void tick();

    default boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return false;
    }

    default boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        return false;
    }

    default boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return false;
    }

    default boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return false;
    }

    default boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return false;
    }

    default boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        return false;
    }

    default boolean charTyped(char pCodePoint, int pModifiers) {        
        return false;
    }

    void onClose();
}
