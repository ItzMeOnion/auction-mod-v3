package com.onxon.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Shared panel, button and click handling for the extra screens (settings, add item). */
abstract class BaseScreen extends Screen {

    protected record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    protected final Screen parent;
    protected final List<Hit> hits = new ArrayList<>();
    protected int px, py, pw, ph;

    protected BaseScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected void layout() {
        pw = Math.min(width - 12, 440);
        ph = Math.min(height - 12, 272);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    /** Panel background, accent line and the two-colour title. */
    protected void drawPanel(DrawContext ctx, String first, String second) {
        Ui.box(ctx, px, py, pw, ph, Ui.PANEL, Ui.BORDER);
        ctx.fill(px + 1, py + 1, px + pw - 1, py + 3, Ui.ACCENT);
        Ui.text(ctx, textRenderer, first, px + 14, py + 9, Ui.RANK1, true);
        int w = Ui.width(textRenderer, first + " ", true);
        Ui.text(ctx, textRenderer, second, px + 14 + w, py + 9, Ui.ACCENT, true);
    }

    protected void button(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label,
                          int fill, int hoverFill, int textColor, int borderColor, boolean enabled, Runnable action) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        Ui.box(ctx, x, y, w, h, hover ? hoverFill : fill, hover ? Ui.WHITE : borderColor);
        String shown = textRenderer.trimToWidth(label, w - 8);
        int lw = textRenderer.getWidth(shown);
        ctx.drawText(textRenderer, shown, x + (w - lw) / 2, y + (h - 8) / 2, enabled ? textColor : Ui.MUTED, true);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    /** Plain grey button in the current theme. */
    protected void plainButton(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label,
                               boolean enabled, Runnable action) {
        button(ctx, mx, my, x, y, w, h, label, Ui.CARD, Ui.CARD_HOVER, Ui.TEXT, Ui.BORDER, enabled, action);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            for (Hit hit : new ArrayList<>(hits)) {
                if (hit.contains(mouseX, mouseY)) {
                    setFocused(null);
                    hit.action().run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void removed() {
        AuctionConfig.save();
    }
}
