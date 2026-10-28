package com.onxon.auction;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

/** Colours (read from AuctionConfig) and small drawing helpers shared by every screen and the HUD. */
public final class Ui {
    private Ui() {}

    // Dynamic colours (ARGB). Call refresh() whenever the config colours change.
    public static int PANEL, CARD, CARD_HOVER, CARD_SELECTED, BORDER;
    public static int ACCENT, TEXT, MUTED;
    public static int RANK1, RANK2, RANK3, RANK_OTHER;

    // Fixed colours
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GREEN = 0xFF3DDC84;
    public static final int RED = 0xFFFF5555;

    static {
        refresh();
    }

    public static void refresh() {
        AuctionConfig c = AuctionConfig.get();
        PANEL = 0xF0000000 | c.panel;
        CARD = 0xFF000000 | c.card;
        BORDER = 0xFF000000 | c.border;
        ACCENT = 0xFF000000 | c.accent;
        TEXT = 0xFF000000 | c.text;
        MUTED = 0xFF000000 | c.muted;
        RANK1 = 0xFF000000 | c.rank1;
        RANK2 = 0xFF000000 | c.rank2;
        RANK3 = 0xFF000000 | c.rank3;
        RANK_OTHER = 0xFF000000 | c.rankOther;
        CARD_HOVER = mix(CARD, WHITE, 0.09);
        CARD_SELECTED = mix(CARD, ACCENT, 0.22);
    }

    public static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Timer colour: good, then warning in the last 10s, then low in the last 5s. */
    public static int timerColor(long remainingMs) {
        AuctionConfig c = AuctionConfig.get();
        int rgb = remainingMs <= 5000 ? c.timerLow : remainingMs <= 10000 ? c.timerWarn : c.timerGood;
        return 0xFF000000 | rgb;
    }

    public static int rankColor(int index) {
        return switch (index) {
            case 0 -> RANK1;
            case 1 -> RANK2;
            case 2 -> RANK3;
            default -> RANK_OTHER;
        };
    }

    public static String rankLabel(int index) {
        return switch (index) {
            case 0 -> "1st";
            case 1 -> "2nd";
            case 2 -> "3rd";
            default -> (index + 1) + "th";
        };
    }

    /** Filled rectangle with a 1px border. */
    public static void box(DrawContext c, int x, int y, int w, int h, int fill, int border) {
        c.fill(x, y, x + w, y + h, border);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    public static Text styled(String s, int color, boolean bold) {
        return Text.literal(s).styled(st -> st.withColor(TextColor.fromRgb(color & 0xFFFFFF)).withBold(bold));
    }

    public static int width(TextRenderer tr, String s, boolean bold) {
        return tr.getWidth(styled(s, WHITE, bold));
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean bold) {
        c.drawText(tr, styled(s, color, bold), x, y, WHITE, true);
    }

    public static void centerText(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean bold) {
        text(c, tr, s, cx - width(tr, s, bold) / 2, y, color, bold);
    }

    public static void rightText(DrawContext c, TextRenderer tr, String s, int rightX, int y, int color, boolean bold) {
        text(c, tr, s, rightX - width(tr, s, bold), y, color, bold);
    }

    /**
     * Draws the leaderboard: 1st, 2nd, 3rd in their rank colours (gold / silver / bronze by default).
     * Returns the height used.
     */
    public static int leaderboard(DrawContext c, TextRenderer tr, int x, int y, int w,
                                  List<AuctionManager.Bid> bids, int maxRows) {
        int shown = Math.min(bids.size(), maxRows);
        for (int i = 0; i < shown; i++) {
            AuctionManager.Bid b = bids.get(i);
            int ry = y + i * 12;
            int col = rankColor(i);
            boolean top = i < 3;
            text(c, tr, rankLabel(i), x, ry, col, top);
            text(c, tr, b.player, x + 28, ry, col, top);
            rightText(c, tr, Money.format(b.amount), x + w, ry, col, top);
        }
        return shown * 12;
    }
}
