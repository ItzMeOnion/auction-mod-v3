package com.onxon.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The main menu: pick an item + time, start an auction, watch it live, browse history, edit the layout. */
public class AuctionScreen extends Screen {

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    // remembered between openings
    private static int selected = 0;        // index into config items; items.size() = the Custom card
    private static int durationChoice = 0;  // 0 = 30s, 1 = 1m, 2 = custom
    private static String timeText = "90";
    private static String itemText = "";

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.systemDefault());

    private static final int CARD_H = 40;
    private static final int GAP = 6;

    private final List<Hit> hits = new ArrayList<>();
    private int tab = 0; // 0 = auction, 1 = history
    private int historyScroll = 0;
    private int gridScroll = 0;
    private boolean confirmClear = false;
    private boolean confirmReset = false;
    private boolean editMode = false;
    private int picked = -1;
    private int px, py, pw, ph;

    private TextFieldWidget customTime;
    private TextFieldWidget customItem;

    public AuctionScreen() {
        super(Text.literal("Onxon's Auction Mod"));
    }

    @Override
    protected void init() {
        layout();

        customTime = new TextFieldWidget(textRenderer, 0, 0, 80, 16, Text.literal("Custom time"));
        customTime.setMaxLength(8);
        customTime.setPlaceholder(Text.literal("90s"));
        customTime.setText(timeText);
        customTime.setChangedListener(s -> timeText = s);

        customItem = new TextFieldWidget(textRenderer, 0, 0, 170, 14, Text.literal("Custom item"));
        customItem.setMaxLength(32);
        customItem.setPlaceholder(Text.literal("Item name..."));
        customItem.setText(itemText);
        customItem.setChangedListener(s -> itemText = s);

        addDrawableChild(customTime);
        addDrawableChild(customItem);
    }

    private void layout() {
        pw = Math.min(width - 12, 440);
        ph = Math.min(height - 12, 272);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        layout();
        customTime.setVisible(false);
        customItem.setVisible(false);

        // panel
        Ui.box(ctx, px, py, pw, ph, Ui.PANEL, Ui.BORDER);
        ctx.fill(px + 1, py + 1, px + pw - 1, py + 3, Ui.ACCENT);

        // title
        Ui.text(ctx, textRenderer, "ONXON'S", px + 14, py + 9, Ui.RANK1, true);
        int tw = Ui.width(textRenderer, "ONXON'S ", true);
        Ui.text(ctx, textRenderer, "AUCTION MOD", px + 14 + tw, py + 9, Ui.ACCENT, true);
        Ui.rightText(ctx, textRenderer, "v1.1", px + pw - 14, py + 9, Ui.MUTED, false);

        boolean running = AuctionManager.state() == AuctionManager.State.RUNNING;

        // tabs + settings
        tabButton(ctx, mx, my, px + 14, py + 22, 92, 16, "New Auction", tab == 0, () -> {
            tab = 0;
            confirmClear = false;
        });
        tabButton(ctx, mx, my, px + 112, py + 22, 72, 16, "History", tab == 1, () -> {
            tab = 1;
            confirmClear = false;
        });
        button(ctx, mx, my, px + pw - 14 - 70, py + 22, 70, 16, "Settings", Ui.CARD, Ui.CARD_HOVER,
                Ui.TEXT, Ui.BORDER, true, () -> {
                    if (client != null) client.setScreen(new SettingsScreen(this));
                });
        if (running) {
            Ui.box(ctx, px + pw - 14 - 70 - 6 - 40, py + 22, 40, 16, 0xFF14301F, Ui.GREEN);
            Ui.centerText(ctx, textRenderer, "LIVE", px + pw - 14 - 70 - 6 - 20, py + 26, Ui.GREEN, true);
        }

        if (tab == 0) {
            if (running) {
                renderRunning(ctx, mx, my);
            } else {
                renderSetup(ctx, mx, my);
            }
        } else {
            renderHistory(ctx, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    // --- tab 0: setup

    private void renderSetup(DrawContext ctx, int mx, int my) {
        AuctionConfig cfg = AuctionConfig.get();
        List<AuctionConfig.ItemEntry> items = cfg.items;
        int n = items.size();
        if (selected > n) selected = n;
        if (picked >= n) picked = -1;

        int left = px + 14;
        int inner = pw - 28;
        int rx = px + pw - 14;

        // ---- heading row
        Ui.text(ctx, textRenderer, "ITEM", left, py + 47, Ui.MUTED, true);
        if (!editMode) {
            if (selected == n) {
                customItem.setX(left + 40);
                customItem.setY(py + 43);
                customItem.setVisible(true);
            }
            button(ctx, mx, my, rx - 80, py + 42, 80, 14, "Edit layout", Ui.CARD, Ui.CARD_HOVER,
                    Ui.TEXT, Ui.BORDER, true, () -> {
                        editMode = true;
                        picked = -1;
                        confirmReset = false;
                    });
        } else {
            int doneX = rx - 44;
            button(ctx, mx, my, doneX, py + 42, 44, 14, "Done", 0xFF1E7A4A, 0xFF28A164, Ui.WHITE, Ui.GREEN, true, () -> {
                editMode = false;
                picked = -1;
                confirmReset = false;
                AuctionConfig.save();
            });
            int plusX = doneX - 6 - 14;
            button(ctx, mx, my, plusX, py + 42, 14, 14, "+", Ui.CARD, Ui.CARD_HOVER, Ui.TEXT, Ui.BORDER,
                    cfg.columns < 7, () -> changeColumns(1));
            int labelW = 38;
            int labelX = plusX - 2 - labelW;
            Ui.centerText(ctx, textRenderer, cfg.columns + " cols", labelX + labelW / 2, py + 46, Ui.TEXT, false);
            int minusX = labelX - 2 - 14;
            button(ctx, mx, my, minusX, py + 42, 14, 14, "-", Ui.CARD, Ui.CARD_HOVER, Ui.TEXT, Ui.BORDER,
                    cfg.columns > 3, () -> changeColumns(-1));
            int resetX = minusX - 6 - 72;
            button(ctx, mx, my, resetX, py + 42, 72, 14, confirmReset ? "Confirm?" : "Reset items",
                    0xFF3A1A1A, 0xFF5A2424, Ui.RED, Ui.RED, true, () -> {
                        if (confirmReset) {
                            cfg.items = AuctionConfig.defaultItems();
                            selected = 0;
                            picked = -1;
                            gridScroll = 0;
                            confirmReset = false;
                            AuctionConfig.save();
                        } else {
                            confirmReset = true;
                        }
                    });
            String hint = picked < 0 ? "Click an item, then click where it goes" : "Now click the spot to move it to";
            int hintMax = resetX - 4 - (left + 34);
            Ui.text(ctx, textRenderer, textRenderer.trimToWidth(hint, hintMax), left + 34, py + 46, Ui.RANK1, false);
        }

        // ---- vertical layout, measured from the bottom
        int startY = py + ph - 12 - 20;
        int timeY = startY - 8 - 16;
        int timeLabelY = timeY - 11;
        int gridTop = py + 60;
        int gridBottom = timeLabelY - 6;

        // ---- item grid
        int cols = cfg.columns;
        int gridW = inner - 8;
        int cardW = (gridW - GAP * (cols - 1)) / cols;
        int total = n + 1; // last slot = Custom (normal) or "+ Add" (edit)
        int visibleRows = Math.max(1, (gridBottom - gridTop + GAP) / (CARD_H + GAP));
        int totalRows = (total + cols - 1) / cols;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        gridScroll = Math.max(0, Math.min(gridScroll, maxScroll));

        int firstSlot = gridScroll * cols;
        int lastSlot = Math.min(total, (gridScroll + visibleRows) * cols);
        for (int s = firstSlot; s < lastSlot; s++) {
            final int idx = s;
            int cx = left + (s % cols) * (cardW + GAP);
            int cy = gridTop + (s / cols - gridScroll) * (CARD_H + GAP);
            boolean hover = mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H;
            boolean special = s == n;

            if (special && editMode) {
                // "+ Add item" card
                Ui.box(ctx, cx, cy, cardW, CARD_H, hover ? Ui.CARD_HOVER : Ui.CARD, hover ? Ui.ACCENT : Ui.BORDER);
                Ui.centerText(ctx, textRenderer, "+", cx + cardW / 2, cy + 10, Ui.ACCENT, true);
                cardLabel(ctx, "Add item", cx, cy, cardW, Ui.ACCENT);
                hits.add(new Hit(cx, cy, cardW, CARD_H, () -> {
                    if (client != null) client.setScreen(new AddItemScreen(this));
                }));
                continue;
            }

            Item icon = special ? Items.NAME_TAG : items.get(s).item();
            String name = special ? "Custom" : items.get(s).name;
            boolean sel = !editMode && s == selected;
            boolean isPicked = editMode && s == picked;

            int fill = sel || isPicked ? Ui.CARD_SELECTED : hover ? Ui.CARD_HOVER : Ui.CARD;
            int border = isPicked ? Ui.RANK1 : sel ? Ui.ACCENT : hover ? Ui.MUTED : Ui.BORDER;
            Ui.box(ctx, cx, cy, cardW, CARD_H, fill, border);
            ctx.drawItem(new ItemStack(icon), cx + (cardW - 16) / 2, cy + 4);
            cardLabel(ctx, name, cx, cy, cardW, sel || isPicked ? Ui.TEXT : Ui.MUTED);

            if (editMode) {
                // small red x to remove this item
                int bx = cx + cardW - 11, by = cy + 2;
                boolean xh = mx >= bx && mx < bx + 9 && my >= by && my < by + 9;
                ctx.fill(bx, by, bx + 9, by + 9, xh ? 0xFFFF5555 : 0xFF8A2A2A);
                ctx.drawText(textRenderer, "x", bx + 2, by + 1, Ui.WHITE, false);
                hits.add(new Hit(bx, by, 9, 9, () -> removeItem(idx)));
                hits.add(new Hit(cx, cy, cardW, CARD_H, () -> pickOrMove(idx)));
            } else {
                hits.add(new Hit(cx, cy, cardW, CARD_H, () -> selected = idx));
            }
        }

        // scrollbar
        if (totalRows > visibleRows) {
            int trackH = gridBottom - gridTop;
            int thumbH = Math.max(12, trackH * visibleRows / totalRows);
            int thumbY = gridTop + (trackH - thumbH) * gridScroll / Math.max(1, maxScroll);
            int bx = left + inner - 4;
            ctx.fill(bx, gridTop, bx + 4, gridBottom, Ui.CARD);
            ctx.fill(bx, thumbY, bx + 4, thumbY + thumbH, Ui.ACCENT);
        }

        // ---- time
        Ui.text(ctx, textRenderer, "TIME", left, timeLabelY, Ui.MUTED, true);
        String[] labels = {"30 sec", "1 min", "Custom"};
        for (int j = 0; j < 3; j++) {
            final int choice = j;
            boolean sel = durationChoice == j;
            button(ctx, mx, my, left + j * 76, timeY, 70, 16, labels[j],
                    sel ? Ui.CARD_SELECTED : Ui.CARD, Ui.CARD_HOVER,
                    sel ? Ui.TEXT : Ui.MUTED, sel ? Ui.ACCENT : Ui.BORDER, true,
                    () -> durationChoice = choice);
        }
        long seconds = currentSeconds();
        boolean secondsOk = validSeconds(seconds);
        if (durationChoice == 2) {
            customTime.setX(left + 228);
            customTime.setY(timeY);
            customTime.setVisible(true);
            if (secondsOk) {
                Ui.text(ctx, textRenderer, "= " + Money.formatDuration(seconds), left + 314, timeY + 4, Ui.GREEN, false);
            } else {
                Ui.text(ctx, textRenderer, "5s to 1h, e.g. 45 / 2m / 1m30s", left + 314, timeY + 4, Ui.RED, false);
            }
        }

        // ---- start
        String itemName = currentItemName();
        boolean valid = secondsOk && !itemName.isBlank() && !editMode;
        String label;
        if (editMode) {
            label = "Press Done to finish editing";
        } else if (valid) {
            label = "START AUCTION   -   " + itemName + "   -   " + Money.formatDuration(seconds);
        } else {
            label = "Pick an item and a valid time";
        }
        button(ctx, mx, my, left, startY, inner, 20, label,
                valid ? 0xFF1E7A4A : Ui.CARD, 0xFF28A164,
                valid ? Ui.WHITE : Ui.MUTED, valid ? Ui.GREEN : Ui.BORDER, valid,
                this::startAuction);
    }

    private void changeColumns(int delta) {
        AuctionConfig cfg = AuctionConfig.get();
        cfg.columns = Math.max(3, Math.min(7, cfg.columns + delta));
        gridScroll = 0;
        AuctionConfig.save();
    }

    private void removeItem(int idx) {
        AuctionConfig cfg = AuctionConfig.get();
        if (idx < 0 || idx >= cfg.items.size()) return;
        cfg.items.remove(idx);
        if (idx < selected) {
            selected--;
        } else if (idx == selected) {
            selected = Math.min(selected, cfg.items.size());
        }
        picked = -1;
        AuctionConfig.save();
    }

    /** Edit mode: first click picks an item up, second click drops it in the clicked spot. */
    private void pickOrMove(int idx) {
        AuctionConfig cfg = AuctionConfig.get();
        List<AuctionConfig.ItemEntry> items = cfg.items;
        if (picked < 0) {
            picked = idx;
            return;
        }
        if (picked == idx) {
            picked = -1;
            return;
        }
        AuctionConfig.ItemEntry selectedEntry = selected < items.size() ? items.get(selected) : null;
        AuctionConfig.ItemEntry moving = items.remove(picked);
        items.add(Math.min(idx, items.size()), moving);
        if (selectedEntry != null) selected = items.indexOf(selectedEntry);
        picked = -1;
        AuctionConfig.save();
    }

    /** Item name under the icon; wraps to two lines when it does not fit. */
    private void cardLabel(DrawContext ctx, String name, int cx, int cy, int cw, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(name), cw - 6);
        int count = Math.min(2, lines.size());
        int startY = count == 1 ? cy + CARD_H - 15 : cy + CARD_H - 21;
        for (int i = 0; i < count; i++) {
            OrderedText line = lines.get(i);
            int lw = textRenderer.getWidth(line);
            ctx.drawText(textRenderer, line, cx + (cw - lw) / 2, startY + i * 9, color, true);
        }
    }

    private long currentSeconds() {
        return switch (durationChoice) {
            case 0 -> 30;
            case 1 -> 60;
            default -> Money.parseDuration(timeText);
        };
    }

    private static boolean validSeconds(long s) {
        return s >= 5 && s <= 3600;
    }

    private String currentItemName() {
        List<AuctionConfig.ItemEntry> items = AuctionConfig.get().items;
        if (selected < items.size()) return items.get(selected).name;
        return itemText.trim();
    }

    private void startAuction() {
        AuctionConfig cfg = AuctionConfig.get();
        long seconds = currentSeconds();
        String name = currentItemName();
        if (!validSeconds(seconds) || name.isBlank()) return;
        boolean custom = selected >= cfg.items.size();
        Item icon = custom ? Items.NAME_TAG : cfg.items.get(selected).item();
        AuctionManager.start(name, icon, custom, seconds);
        if (cfg.closeOnStart) close();
    }

    // --- tab 0: running

    private void renderRunning(DrawContext ctx, int mx, int my) {
        int left = px + 14;
        int inner = pw - 28;

        ctx.drawItem(new ItemStack(AuctionManager.itemIcon()), left, py + 46);
        Ui.text(ctx, textRenderer, AuctionManager.itemName(), left + 22, py + 50, Ui.TEXT, true);
        Ui.rightText(ctx, textRenderer, "Duration " + Money.formatDuration(AuctionManager.durationSeconds()),
                px + pw - 14, py + 50, Ui.MUTED, false);

        long remaining = AuctionManager.remainingMs();
        int timeColor = Ui.timerColor(remaining);
        Ui.centerText(ctx, textRenderer, Money.clock(remaining), px + pw / 2, py + 70, timeColor, true);

        int barY = py + 84;
        ctx.fill(left, barY, left + inner, barY + 6, Ui.CARD_HOVER);
        ctx.fill(left, barY, left + (int) (inner * (1.0 - AuctionManager.progress())), barY + 6, timeColor);

        String stats = AuctionManager.payments() + " payments   |   " + AuctionManager.players()
                + " players   |   total " + Money.format(AuctionManager.totalPaid());
        Ui.centerText(ctx, textRenderer, stats, px + pw / 2, py + 97, Ui.MUTED, false);

        Ui.text(ctx, textRenderer, "LEADERBOARD", left, py + 114, Ui.MUTED, true);
        Ui.rightText(ctx, textRenderer, "highest single payment wins", px + pw - 14, py + 114, Ui.MUTED, false);

        int rowsTop = py + 128;
        int buttonsY = py + ph - 12 - 20;
        int maxRows = Math.max(1, Math.min(8, (buttonsY - 6 - rowsTop) / 12));
        List<AuctionManager.Bid> board = AuctionManager.leaderboard();
        if (board.isEmpty()) {
            Ui.text(ctx, textRenderer, "Waiting for payments...", left, rowsTop, Ui.MUTED, false);
        } else {
            Ui.leaderboard(ctx, textRenderer, left, rowsTop, inner, board, maxRows);
        }

        int half = (inner - 6) / 2;
        button(ctx, mx, my, left, buttonsY, half, 20, "End now", 0xFF4A3A12, 0xFF6B5418, Ui.RANK1, Ui.RANK1, true,
                AuctionManager::endNow);
        button(ctx, mx, my, left + half + 6, buttonsY, half, 20, "Cancel auction", 0xFF4A1A1A, 0xFF6B2424, Ui.RED,
                Ui.RED, true, AuctionManager::cancel);
    }

    // --- tab 1: history

    private void renderHistory(DrawContext ctx, int mx, int my) {
        int left = px + 14;
        int inner = pw - 28;
        List<AuctionRecord> records = AuctionHistory.all();

        long totalPaid = 0;
        long best = 0;
        int totalPayments = 0;
        for (AuctionRecord r : records) {
            totalPaid += r.totalPaid;
            totalPayments += r.payments;
            best = Math.max(best, r.winningAmount);
        }
        String summary = records.size() + " auctions   |   " + totalPayments + " payments   |   "
                + Money.format(totalPaid) + " total   |   best " + Money.format(best);
        Ui.text(ctx, textRenderer, summary, left, py + 47, Ui.MUTED, false);

        int listTop = py + 60;
        int listBottom = py + ph - 34;
        int rowH = 34;
        int visible = Math.max(1, (listBottom - listTop) / rowH);
        int maxScroll = Math.max(0, records.size() - visible);
        historyScroll = Math.max(0, Math.min(historyScroll, maxScroll));

        if (records.isEmpty()) {
            Ui.centerText(ctx, textRenderer, "No auctions yet - start one!", px + pw / 2, listTop + 30, Ui.MUTED, false);
        }

        for (int i = 0; i < visible; i++) {
            int idx = historyScroll + i;
            if (idx >= records.size()) break;
            AuctionRecord r = records.get(idx);
            int y = listTop + i * rowH;
            Ui.box(ctx, left, y, inner, rowH - 3, Ui.CARD, Ui.BORDER);

            ctx.drawItem(new ItemStack(AuctionConfig.iconOf(r.itemId, r.item)), left + 6, y + 7);
            Ui.text(ctx, textRenderer, r.item, left + 30, y + 4, Ui.TEXT, true);
            Ui.rightText(ctx, textRenderer, DATE.format(Instant.ofEpochMilli(r.endedAt)), left + inner - 6, y + 4,
                    Ui.MUTED, false);

            if (r.winner != null) {
                Ui.text(ctx, textRenderer, "Winner: ", left + 30, y + 14, Ui.MUTED, false);
                int ww = Ui.width(textRenderer, "Winner: ", false);
                String paid = r.winner + " paid " + Money.format(r.winningAmount);
                Ui.text(ctx, textRenderer, paid, left + 30 + ww, y + 14, Ui.RANK1, true);
            } else {
                Ui.text(ctx, textRenderer, "Nobody paid", left + 30, y + 14, Ui.MUTED, false);
            }

            String details = r.payments + " payments  |  " + r.players + " players  |  total "
                    + Money.format(r.totalPaid) + "  |  " + Money.formatDuration(r.durationSeconds);
            Ui.text(ctx, textRenderer, details, left + 30, y + 23, Ui.MUTED, false);
        }

        int footY = py + ph - 12 - 18;
        if (!records.isEmpty()) {
            String label = confirmClear ? "Click again to confirm" : "Clear history";
            button(ctx, mx, my, left, footY, 120, 18, label, 0xFF3A1A1A, 0xFF5A2424, Ui.RED, Ui.RED, true, () -> {
                if (confirmClear) {
                    AuctionHistory.clear();
                    confirmClear = false;
                    historyScroll = 0;
                } else {
                    confirmClear = true;
                }
            });
            if (records.size() > visible) {
                int first = historyScroll + 1;
                int last = Math.min(records.size(), historyScroll + visible);
                Ui.rightText(ctx, textRenderer, "Showing " + first + "-" + last + " of " + records.size()
                        + "  (scroll)", px + pw - 14, footY + 5, Ui.MUTED, false);
            }
        }
    }

    // ------------------------------------------------------------------ widgets

    private void tabButton(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label,
                           boolean active, Runnable action) {
        button(ctx, mx, my, x, y, w, h, label,
                active ? Ui.CARD_SELECTED : Ui.CARD, Ui.CARD_HOVER,
                active ? Ui.TEXT : Ui.MUTED, active ? Ui.ACCENT : Ui.BORDER, true, action);
    }

    private void button(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label,
                        int fill, int hoverFill, int textColor, int borderColor, boolean enabled, Runnable action) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        Ui.box(ctx, x, y, w, h, hover ? hoverFill : fill, hover ? Ui.WHITE : borderColor);
        String shown = textRenderer.trimToWidth(label, w - 8);
        int lw = textRenderer.getWidth(shown);
        ctx.drawText(textRenderer, shown, x + (w - lw) / 2, y + (h - 8) / 2, enabled ? textColor : Ui.MUTED, true);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    // ------------------------------------------------------------------ input

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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = (int) Math.signum(verticalAmount);
        if (tab == 1) {
            historyScroll -= step;
            return true;
        }
        if (tab == 0 && AuctionManager.state() != AuctionManager.State.RUNNING) {
            gridScroll -= step;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = getFocused() instanceof TextFieldWidget;
        if (!typing && OnxonAuctionMod.openKey != null && OnxonAuctionMod.openKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        AuctionConfig.save();
    }
}
