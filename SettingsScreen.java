package com.onxon.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Customise colours (menu, leaderboard, chat) and quality-of-life options. Everything saves automatically. */
public class SettingsScreen extends BaseScreen {

    private record ColourRow(String label, Function<AuctionConfig, Integer> get, BiConsumer<AuctionConfig, Integer> set) {}

    private static final String[] TABS = {"Menu", "Leaderboard", "Chat", "General", "Announce"};
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    private static final int[] RESULT_SECONDS = {10, 20, 30, 60};
    private static final int[] PALETTE = {
            0xFF6FAE, 0xFFD54A, 0xC8CCD4, 0xCD7F32, 0x3DDC84, 0xFF5555, 0x55AAFF, 0xB084FF,
            0xFF9F43, 0x4DE0E0, 0xFFFFFF, 0x9A9AB0, 0x101015, 0x1E1E29, 0x34344A, 0x000000
    };

    private final List<List<ColourRow>> colourTabs = new ArrayList<>();
    private int tab = 0;
    private int editing = 0;
    private boolean suppress = false;

    private TextFieldWidget hexField;
    private TextFieldWidget startField;
    private TextFieldWidget winField;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"), parent);

        colourTabs.add(List.of(
                new ColourRow("Accent / highlight", c -> c.accent, (c, v) -> c.accent = v),
                new ColourRow("Panel background", c -> c.panel, (c, v) -> c.panel = v),
                new ColourRow("Card background", c -> c.card, (c, v) -> c.card = v),
                new ColourRow("Borders", c -> c.border, (c, v) -> c.border = v),
                new ColourRow("Main text", c -> c.text, (c, v) -> c.text = v),
                new ColourRow("Muted text", c -> c.muted, (c, v) -> c.muted = v)
        ));
        colourTabs.add(List.of(
                new ColourRow("1st place", c -> c.rank1, (c, v) -> c.rank1 = v),
                new ColourRow("2nd place", c -> c.rank2, (c, v) -> c.rank2 = v),
                new ColourRow("3rd place", c -> c.rank3, (c, v) -> c.rank3 = v),
                new ColourRow("Other places", c -> c.rankOther, (c, v) -> c.rankOther = v),
                new ColourRow("Timer: plenty of time", c -> c.timerGood, (c, v) -> c.timerGood = v),
                new ColourRow("Timer: under 10s", c -> c.timerWarn, (c, v) -> c.timerWarn = v),
                new ColourRow("Timer: under 5s", c -> c.timerLow, (c, v) -> c.timerLow = v)
        ));
        colourTabs.add(List.of(
                new ColourRow("[Auction] prefix", c -> c.chatPrefix, (c, v) -> c.chatPrefix = v),
                new ColourRow("Message text", c -> c.chatText, (c, v) -> c.chatText = v),
                new ColourRow("Winner line", c -> c.chatWinner, (c, v) -> c.chatWinner = v),
                new ColourRow("Info lines", c -> c.chatInfo, (c, v) -> c.chatInfo = v)
        ));
    }

    @Override
    protected void init() {
        layout();
        AuctionConfig cfg = AuctionConfig.get();

        hexField = new TextFieldWidget(textRenderer, 0, 0, 74, 16, Text.literal("Hex colour"));
        hexField.setMaxLength(7);
        hexField.setChangedListener(s -> {
            if (suppress) return;
            Integer v = parseHex(s);
            if (v != null) setCurrent(v);
        });

        startField = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Start message"));
        startField.setMaxLength(200);
        startField.setText(cfg.startTemplate);
        startField.setChangedListener(s -> AuctionConfig.get().startTemplate = s);

        winField = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Winner message"));
        winField.setMaxLength(200);
        winField.setText(cfg.winnerTemplate);
        winField.setChangedListener(s -> AuctionConfig.get().winnerTemplate = s);

        addDrawableChild(hexField);
        addDrawableChild(startField);
        addDrawableChild(winField);
        syncHex();
    }

    // ------------------------------------------------------------------ colour helpers

    private List<ColourRow> rows() {
        return colourTabs.get(Math.min(tab, colourTabs.size() - 1));
    }

    private int current() {
        return rows().get(editing).get().apply(AuctionConfig.get());
    }

    private void setCurrent(int rgb) {
        rows().get(editing).set().accept(AuctionConfig.get(), rgb & 0xFFFFFF);
        Ui.refresh();
    }

    private void syncHex() {
        if (hexField == null || tab > 2) return;
        suppress = true;
        hexField.setText(String.format("#%06X", current() & 0xFFFFFF));
        suppress = false;
    }

    private static Integer parseHex(String s) {
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() != 6) return null;
        try {
            return Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void switchTab(int t) {
        tab = t;
        editing = 0;
        syncHex();
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        layout();
        hexField.setVisible(false);
        startField.setVisible(false);
        winField.setVisible(false);

        drawPanel(ctx, "ONXON'S", "SETTINGS");
        Ui.rightText(ctx, textRenderer, "changes save automatically", px + pw - 14, py + 9, Ui.MUTED, false);

        int left = px + 14;
        int inner = pw - 28;

        // tabs
        int tabW = (inner - 4 * 4) / 5;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            boolean active = tab == i;
            button(ctx, mx, my, left + i * (tabW + 4), py + 22, tabW, 16, TABS[i],
                    active ? Ui.CARD_SELECTED : Ui.CARD, Ui.CARD_HOVER,
                    active ? Ui.TEXT : Ui.MUTED, active ? Ui.ACCENT : Ui.BORDER, true, () -> switchTab(idx));
        }

        if (tab <= 2) {
            renderColours(ctx, mx, my, left, inner);
        } else if (tab == 3) {
            renderGeneral(ctx, mx, my, left, inner);
        } else {
            renderAnnounce(ctx, mx, my, left, inner);
        }

        plainButton(ctx, mx, my, left, py + ph - 12 - 18, 70, 18, "Back", true, this::close);

        super.render(ctx, mx, my, delta);
    }

    // --- colour tabs

    private void renderColours(DrawContext ctx, int mx, int my, int left, int inner) {
        List<ColourRow> rows = rows();
        if (editing >= rows.size()) editing = 0;
        AuctionConfig cfg = AuctionConfig.get();

        int rowsTop = py + 46;
        int rowW = 212;
        int rowH = 17;
        for (int i = 0; i < rows.size(); i++) {
            final int idx = i;
            ColourRow row = rows.get(i);
            int y = rowsTop + i * (rowH + 2);
            boolean sel = i == editing;
            boolean hover = mx >= left && mx < left + rowW && my >= y && my < y + rowH;
            Ui.box(ctx, left, y, rowW, rowH, sel ? Ui.CARD_SELECTED : hover ? Ui.CARD_HOVER : Ui.CARD,
                    sel ? Ui.ACCENT : Ui.BORDER);
            int value = row.get().apply(cfg);
            Ui.box(ctx, left + 4, y + 3, 12, 11, 0xFF000000 | value, Ui.MUTED);
            Ui.text(ctx, textRenderer, row.label(), left + 22, y + 5, Ui.TEXT, false);
            Ui.rightText(ctx, textRenderer, String.format("#%06X", value & 0xFFFFFF), left + rowW - 5, y + 5, Ui.MUTED, false);
            hits.add(new Hit(left, y, rowW, rowH, () -> {
                editing = idx;
                syncHex();
            }));
        }

        // editor (bottom-up)
        int backY = py + ph - 12 - 18;
        int hexY = backY - 6 - 16;
        int palY = hexY - 6 - 14;

        int cur = current() & 0xFFFFFF;
        for (int k = 0; k < PALETTE.length; k++) {
            final int rgb = PALETTE[k];
            int sx = left + k * 21;
            boolean isCur = rgb == cur;
            Ui.box(ctx, sx, palY, 18, 14, 0xFF000000 | rgb, isCur ? Ui.WHITE : Ui.BORDER);
            hits.add(new Hit(sx, palY, 18, 14, () -> {
                setCurrent(rgb);
                syncHex();
            }));
        }

        Ui.text(ctx, textRenderer, "Hex", left, hexY + 4, Ui.MUTED, false);
        hexField.setX(left + 24);
        hexField.setY(hexY);
        hexField.setVisible(true);
        plainButton(ctx, mx, my, left + 106, hexY, 84, 16, "Reset colour", true, () -> {
            ColourRow row = rows().get(editing);
            setCurrent(row.get().apply(new AuctionConfig()));
            syncHex();
        });
        plainButton(ctx, mx, my, left + 196, hexY, 110, 16, "Reset all colours", true, () -> {
            AuctionConfig.resetColours();
            syncHex();
        });

        // live preview on the right
        int prevX = left + rowW + 12;
        int prevW = inner - rowW - 12;
        int prevH = palY - 8 - rowsTop;
        if (tab == 0) {
            previewMenu(ctx, prevX, rowsTop, prevW, prevH);
        } else if (tab == 1) {
            previewLeaderboard(ctx, prevX, rowsTop, prevW, prevH);
        } else {
            previewChat(ctx, prevX, rowsTop, prevW, prevH);
        }
    }

    private void previewMenu(DrawContext ctx, int x, int y, int w, int h) {
        Ui.text(ctx, textRenderer, "PREVIEW", x, y, Ui.MUTED, true);
        int top = y + 12;
        Ui.box(ctx, x, top, w, h - 12, Ui.PANEL, Ui.BORDER);
        ctx.fill(x + 1, top + 1, x + w - 1, top + 3, Ui.ACCENT);
        Ui.text(ctx, textRenderer, "ONXON'S", x + 8, top + 8, Ui.RANK1, true);
        Ui.text(ctx, textRenderer, "AUCTION MOD", x + 8 + Ui.width(textRenderer, "ONXON'S ", true), top + 8, Ui.ACCENT, true);

        int cw = (w - 16 - 6) / 2;
        Ui.box(ctx, x + 8, top + 22, cw, 30, Ui.CARD_SELECTED, Ui.ACCENT);
        ctx.drawItem(new ItemStack(Items.ELYTRA), x + 8 + (cw - 16) / 2, top + 24);
        Ui.centerText(ctx, textRenderer, "Elytra", x + 8 + cw / 2, top + 42, Ui.TEXT, false);
        Ui.box(ctx, x + 14 + cw, top + 22, cw, 30, Ui.CARD, Ui.BORDER);
        ctx.drawItem(new ItemStack(Items.SPAWNER), x + 14 + cw + (cw - 16) / 2, top + 24);
        Ui.centerText(ctx, textRenderer, "Spawner", x + 14 + cw + cw / 2, top + 42, Ui.MUTED, false);

        Ui.box(ctx, x + 8, top + 58, w - 16, 16, 0xFF1E7A4A, Ui.GREEN);
        Ui.centerText(ctx, textRenderer, "START AUCTION", x + w / 2, top + 62, Ui.WHITE, true);
        Ui.text(ctx, textRenderer, "Main text", x + 8, top + 80, Ui.TEXT, false);
        Ui.text(ctx, textRenderer, "Muted text", x + 8, top + 91, Ui.MUTED, false);
    }

    private void previewLeaderboard(DrawContext ctx, int x, int y, int w, int h) {
        Ui.text(ctx, textRenderer, "PREVIEW", x, y, Ui.MUTED, true);
        int top = y + 12;
        Ui.box(ctx, x, top, w, h - 12, Ui.PANEL, Ui.BORDER);
        ctx.fill(x + 1, top + 1, x + w - 1, top + 3, Ui.ACCENT);

        List<AuctionManager.Bid> sample = List.of(
                new AuctionManager.Bid("Onxon", 100_000_000L, 0),
                new AuctionManager.Bid("Steve", 75_000_000L, 0),
                new AuctionManager.Bid("Alex", 50_000_000L, 0),
                new AuctionManager.Bid("Herobrine", 10_000_000L, 0));
        Ui.leaderboard(ctx, textRenderer, x + 8, top + 8, w - 16, sample, 4);

        int by = top + 62;
        long[] times = {20000, 8000, 3000};
        for (int i = 0; i < 3; i++) {
            int c = Ui.timerColor(times[i]);
            ctx.fill(x + 8, by + i * 8, x + 8 + (w - 16) * (3 - i) / 3, by + i * 8 + 4, c);
        }
    }

    private void previewChat(DrawContext ctx, int x, int y, int w, int h) {
        AuctionConfig cfg = AuctionConfig.get();
        Ui.text(ctx, textRenderer, "PREVIEW", x, y, Ui.MUTED, true);
        int top = y + 12;
        ctx.fill(x, top, x + w, top + h - 12, 0xB0000000);

        MutableText prefix = Ui.styled("[Auction] ", 0xFF000000 | cfg.chatPrefix, false).copy();
        List<Text> lines = new ArrayList<>();
        lines.add(Ui.styled("[Auction] ", 0xFF000000 | cfg.chatPrefix, false).copy()
                .append(Ui.styled("Auction started: ", 0xFF000000 | cfg.chatText, false))
                .append(Ui.styled("Elytra (30s)", 0xFF000000 | cfg.chatText, false)));
        lines.add(Ui.styled("[Auction] ", 0xFF000000 | cfg.chatPrefix, false).copy()
                .append(Ui.styled("Auction ended: Elytra", 0xFF000000 | cfg.chatPrefix, false)));
        lines.add(Ui.styled("[Auction] ", 0xFF000000 | cfg.chatPrefix, false).copy()
                .append(Ui.styled("Winner: Onxon paid $100M", 0xFF000000 | cfg.chatWinner, true)));
        lines.add(Ui.styled("[Auction] ", 0xFF000000 | cfg.chatPrefix, false).copy()
                .append(Ui.styled("Nobody paid.", 0xFF000000 | cfg.chatInfo, false)));
        int ly = top + 4;
        for (Text t : lines) {
            List<OrderedText> wrapped = textRenderer.wrapLines(t, w - 8);
            for (OrderedText o : wrapped) {
                ctx.drawText(textRenderer, o, x + 4, ly, Ui.WHITE, true);
                ly += 10;
            }
            ly += 2;
        }
    }

    // --- general tab

    private void renderGeneral(DrawContext ctx, int mx, int my, int left, int inner) {
        AuctionConfig cfg = AuctionConfig.get();
        int y = py + 46;
        int step = 24;

        toggleRow(ctx, mx, my, left, y, inner, "Show timer + leaderboard on screen", cfg.showHud,
                () -> cfg.showHud = !cfg.showHud);
        y += step;
        valueRow(ctx, mx, my, left, y, inner, "Timer position", CORNERS[Math.max(0, Math.min(3, cfg.hudCorner))],
                () -> cfg.hudCorner = (cfg.hudCorner + 1) % 4);
        y += step;
        valueRow(ctx, mx, my, left, y, inner, "Result panel stays for", cfg.resultSeconds + " seconds", () -> {
            int next = RESULT_SECONDS[0];
            for (int i = 0; i < RESULT_SECONDS.length; i++) {
                if (RESULT_SECONDS[i] == cfg.resultSeconds) {
                    next = RESULT_SECONDS[(i + 1) % RESULT_SECONDS.length];
                    break;
                }
            }
            cfg.resultSeconds = next;
        });
        y += step;
        toggleRow(ctx, mx, my, left, y, inner, "Chat messages (only you see them)", cfg.chatMessages,
                () -> cfg.chatMessages = !cfg.chatMessages);
        y += step;
        toggleRow(ctx, mx, my, left, y, inner, "Sound when the auction ends", cfg.sound,
                () -> cfg.sound = !cfg.sound);
        y += step;
        toggleRow(ctx, mx, my, left, y, inner, "Countdown beeps in the last 5 seconds", cfg.countdownBeeps,
                () -> cfg.countdownBeeps = !cfg.countdownBeeps);
        y += step;
        toggleRow(ctx, mx, my, left, y, inner, "Close the menu when an auction starts", cfg.closeOnStart,
                () -> cfg.closeOnStart = !cfg.closeOnStart);
    }

    // --- announce tab

    private void renderAnnounce(DrawContext ctx, int mx, int my, int left, int inner) {
        AuctionConfig cfg = AuctionConfig.get();
        int y = py + 46;

        toggleRow(ctx, mx, my, left, y, inner, "Announce the start in public chat", cfg.announceStart,
                () -> cfg.announceStart = !cfg.announceStart);
        y += 24;
        toggleRow(ctx, mx, my, left, y, inner, "Announce the winner in public chat", cfg.announceWinner,
                () -> cfg.announceWinner = !cfg.announceWinner);
        y += 30;

        Ui.text(ctx, textRenderer, "Start message", left, y, Ui.MUTED, true);
        Ui.rightText(ctx, textRenderer, "{item}  {time}", left + inner, y, Ui.MUTED, false);
        startField.setX(left);
        startField.setY(y + 11);
        startField.setWidth(inner);
        startField.setVisible(true);
        y += 36;

        Ui.text(ctx, textRenderer, "Winner message", left, y, Ui.MUTED, true);
        Ui.rightText(ctx, textRenderer, "{winner}  {item}  {amount}", left + inner, y, Ui.MUTED, false);
        winField.setX(left);
        winField.setY(y + 11);
        winField.setWidth(inner);
        winField.setVisible(true);
        y += 36;

        List<OrderedText> note = textRenderer.wrapLines(Text.literal(
                "These send a normal chat message as you, so use them how the server allows. "
                        + "Both are off by default. A message starting with / is never sent."), inner);
        for (int i = 0; i < Math.min(2, note.size()); i++) {
            ctx.drawText(textRenderer, note.get(i), left, y + i * 10, Ui.MUTED, false);
        }
    }

    private void toggleRow(DrawContext ctx, int mx, int my, int left, int y, int inner, String label, boolean on,
                           Runnable flip) {
        Ui.box(ctx, left, y, inner, 20, Ui.CARD, Ui.BORDER);
        Ui.text(ctx, textRenderer, label, left + 8, y + 6, Ui.TEXT, false);
        button(ctx, mx, my, left + inner - 64, y + 2, 58, 16, on ? "ON" : "OFF",
                on ? 0xFF14301F : 0xFF3A1A1A, on ? 0xFF1E4A2F : 0xFF5A2424,
                on ? Ui.GREEN : Ui.RED, on ? Ui.GREEN : Ui.RED, true, flip);
    }

    private void valueRow(DrawContext ctx, int mx, int my, int left, int y, int inner, String label, String value,
                          Runnable next) {
        Ui.box(ctx, left, y, inner, 20, Ui.CARD, Ui.BORDER);
        Ui.text(ctx, textRenderer, label, left + 8, y + 6, Ui.TEXT, false);
        button(ctx, mx, my, left + inner - 104, y + 2, 98, 16, value, Ui.CARD_SELECTED, Ui.CARD_HOVER,
                Ui.TEXT, Ui.ACCENT, true, next);
    }
}
