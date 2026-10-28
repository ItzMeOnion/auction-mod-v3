package com.onxon.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

import java.util.List;

/** The on-screen timer + live leaderboard, and the result panel after an auction ends. */
public final class AuctionHud {
    private AuctionHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;

        AuctionManager.State state = AuctionManager.state();
        if (state == AuctionManager.State.RUNNING) {
            if (AuctionConfig.get().showHud) drawRunning(ctx, tr);
        } else if (state == AuctionManager.State.FINISHED && AuctionManager.resultVisible()) {
            drawResult(ctx, tr);
        }
    }

    private static void drawRunning(DrawContext ctx, TextRenderer tr) {
        AuctionConfig cfg = AuctionConfig.get();
        int w = 180;
        List<AuctionManager.Bid> board = AuctionManager.leaderboard();
        int rows = Math.max(1, Math.min(5, board.size()));
        int h = 52 + rows * 12 + 18;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        boolean right = cfg.hudCorner == 1 || cfg.hudCorner == 3;
        boolean bottom = cfg.hudCorner >= 2;
        int x = right ? sw - w - 6 : 6;
        int y = bottom ? sh - h - 6 : 6;

        Ui.box(ctx, x, y, w, h, Ui.PANEL, Ui.BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 3, Ui.ACCENT);

        // header: icon + item name
        ctx.drawItem(new ItemStack(AuctionManager.itemIcon()), x + 6, y + 6);
        String name = tr.trimToWidth(AuctionManager.itemName(), w - 36);
        Ui.text(ctx, tr, name, x + 28, y + 8, Ui.TEXT, true);
        Ui.text(ctx, tr, "AUCTION", x + 28, y + 18, Ui.ACCENT, true);

        // timer
        long remaining = AuctionManager.remainingMs();
        int timeColor = Ui.timerColor(remaining);
        Ui.text(ctx, tr, "Time left", x + 8, y + 31, Ui.MUTED, false);
        Ui.rightText(ctx, tr, Money.clock(remaining), x + w - 8, y + 31, timeColor, true);

        // progress bar (drains as time runs out)
        int barX = x + 8, barY = y + 42, barW = w - 16;
        ctx.fill(barX, barY, barX + barW, barY + 4, Ui.CARD_HOVER);
        int filled = (int) (barW * (1.0 - AuctionManager.progress()));
        ctx.fill(barX, barY, barX + filled, barY + 4, timeColor);

        // leaderboard
        int ly = y + 52;
        if (board.isEmpty()) {
            Ui.text(ctx, tr, "Waiting for payments...", x + 8, ly, Ui.MUTED, false);
        } else {
            Ui.leaderboard(ctx, tr, x + 8, ly, w - 16, board, 5);
        }

        // footer
        String footer = AuctionManager.payments() + " payments  |  " + AuctionManager.players() + " players";
        Ui.text(ctx, tr, footer, x + 8, y + h - 13, Ui.MUTED, false);
    }

    private static void drawResult(DrawContext ctx, TextRenderer tr) {
        AuctionRecord r = AuctionManager.lastResult();
        if (r == null) return;

        int w = 240;
        int x = (ctx.getScaledWindowWidth() - w) / 2;
        int y = 10;
        boolean hasWinner = r.winner != null;
        int h = 30 + (hasWinner ? 36 : 18) + 18;
        int cx = x + w / 2;

        Ui.box(ctx, x, y, w, h, Ui.PANEL, Ui.RANK1);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 3, Ui.RANK1);

        ctx.drawItem(new ItemStack(AuctionConfig.iconOf(r.itemId, r.item)), x + 8, y + 8);
        Ui.text(ctx, tr, "AUCTION ENDED", x + 30, y + 9, Ui.ACCENT, true);
        Ui.text(ctx, tr, tr.trimToWidth(r.item, w - 40), x + 30, y + 19, Ui.MUTED, false);

        int cy = y + 32;
        if (hasWinner) {
            Ui.centerText(ctx, tr, "WINNER", cx, cy, Ui.MUTED, false);
            Ui.centerText(ctx, tr, r.winner, cx, cy + 11, Ui.RANK1, true);
            Ui.centerText(ctx, tr, "paid " + Money.format(r.winningAmount), cx, cy + 23, Ui.RANK1, true);
            cy += 36;
        } else {
            Ui.centerText(ctx, tr, "Nobody paid", cx, cy + 2, Ui.MUTED, true);
            cy += 18;
        }

        String stats = r.payments + " payments  |  " + r.players + " players  |  total " + Money.format(r.totalPaid);
        Ui.centerText(ctx, tr, stats, cx, cy + 2, Ui.MUTED, false);
    }
}
