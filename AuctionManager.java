package com.onxon.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Holds the running auction, tracks incoming payments and decides the winner. */
public final class AuctionManager {
    private AuctionManager() {}

    public enum State { IDLE, RUNNING, FINISHED }

    /** A player's best single payment during the auction. */
    public static final class Bid {
        public final String player;
        public final long amount;
        public final long at;

        Bid(String player, long amount, long at) {
            this.player = player;
            this.amount = amount;
            this.at = at;
        }
    }

    private static State state = State.IDLE;
    private static String itemName = "";
    private static String itemId = null;
    private static Item itemIcon = Items.NAME_TAG;
    private static long startMs;
    private static long endMs;
    private static long durationSec;

    /** lower-case name -> that player's highest single payment */
    private static final Map<String, Bid> bids = new HashMap<>();
    private static int payments;
    private static long totalPaid;

    private static AuctionRecord lastResult;
    private static long finishedAt;
    private static int lastBeepSecond = -1;

    // used to ignore the same payment arriving twice (chat + action bar)
    private static String lastKey = "";
    private static long lastKeyAt;
    private static boolean lastOverlay;

    // ---- getters ----
    public static State state() { return state; }
    public static String itemName() { return itemName; }
    public static Item itemIcon() { return itemIcon; }
    public static long durationSeconds() { return durationSec; }
    public static int payments() { return payments; }
    public static int players() { return bids.size(); }
    public static long totalPaid() { return totalPaid; }
    public static AuctionRecord lastResult() { return lastResult; }

    public static long remainingMs() {
        return Math.max(0, endMs - System.currentTimeMillis());
    }

    /** 0.0 (just started) to 1.0 (finished) */
    public static double progress() {
        long total = endMs - startMs;
        if (total <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, (System.currentTimeMillis() - startMs) / (double) total));
    }

    public static boolean resultVisible() {
        return state == State.FINISHED && lastResult != null
                && System.currentTimeMillis() - finishedAt < AuctionConfig.get().resultSeconds * 1000L;
    }

    public static void dismissResult() {
        if (state == State.FINISHED) state = State.IDLE;
    }

    /** Leaderboard: highest single payment first. Ties go to whoever paid first. */
    public static List<Bid> leaderboard() {
        List<Bid> list = new ArrayList<>(bids.values());
        list.sort((a, b) -> {
            int c = Long.compare(b.amount, a.amount);
            return c != 0 ? c : Long.compare(a.at, b.at);
        });
        return list;
    }

    // ---- control ----
    public static void start(String name, Item icon, boolean customItem, long seconds) {
        bids.clear();
        payments = 0;
        totalPaid = 0;
        lastKey = "";
        lastBeepSecond = -1;
        itemName = name;
        itemIcon = icon;
        itemId = customItem ? null : AuctionConfig.idOf(icon);
        durationSec = seconds;
        startMs = System.currentTimeMillis();
        endMs = startMs + seconds * 1000L;
        state = State.RUNNING;

        AuctionConfig cfg = AuctionConfig.get();
        say(line("Auction started: ", cfg.chatText)
                .append(line(name + " (" + Money.formatDuration(seconds) + ")", cfg.chatText)));
        if (cfg.announceStart) {
            publicChat(fill(cfg.startTemplate, name, Money.formatDuration(seconds), "", ""));
        }
    }

    public static void cancel() {
        if (state != State.RUNNING) return;
        state = State.IDLE;
        say(line("Auction cancelled.", AuctionConfig.get().chatInfo));
    }

    public static void endNow() {
        if (state == State.RUNNING) finish();
    }

    public static void tick() {
        if (state != State.RUNNING) return;
        long remaining = endMs - System.currentTimeMillis();
        if (remaining <= 0) {
            finish();
            return;
        }
        AuctionConfig cfg = AuctionConfig.get();
        int secondsLeft = (int) ((remaining + 999) / 1000);
        if (cfg.countdownBeeps && cfg.sound && secondsLeft <= 5 && secondsLeft != lastBeepSecond && durationSec > 5) {
            lastBeepSecond = secondsLeft;
            play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, secondsLeft == 1 ? 1.4f : 1.0f);
        }
    }

    // ---- payments ----
    public static void onMessage(Text message, boolean overlay) {
        if (state != State.RUNNING) return;
        long now = System.currentTimeMillis();
        if (now > endMs) return;

        Money.Payment p = Money.parsePayment(message.getString());
        if (p == null) return;

        // The same payment can show up in chat and on the action bar: count it once.
        String key = p.player().toLowerCase(Locale.ROOT) + ":" + p.amount();
        if (key.equals(lastKey) && overlay != lastOverlay && now - lastKeyAt < 1500) return;
        lastKey = key;
        lastKeyAt = now;
        lastOverlay = overlay;

        payments++;
        totalPaid += p.amount();

        String id = p.player().toLowerCase(Locale.ROOT);
        Bid existing = bids.get(id);
        // Only the highest single payment per player counts for the ranking.
        if (existing == null || p.amount() > existing.amount) {
            bids.put(id, new Bid(p.player(), p.amount(), now));
        }
    }

    private static void finish() {
        long now = System.currentTimeMillis();
        List<Bid> board = leaderboard();

        AuctionRecord r = new AuctionRecord();
        r.item = itemName;
        r.itemId = itemId;
        r.durationSeconds = durationSec;
        r.endedAt = now;
        r.payments = payments;
        r.players = bids.size();
        r.totalPaid = totalPaid;
        if (!board.isEmpty()) {
            r.winner = board.get(0).player;
            r.winningAmount = board.get(0).amount;
        }
        for (int i = 0; i < Math.min(10, board.size()); i++) {
            r.top.add(new AuctionRecord.TopEntry(board.get(i).player, board.get(i).amount));
        }

        AuctionHistory.add(r);
        lastResult = r;
        finishedAt = now;
        state = State.FINISHED;

        AuctionConfig cfg = AuctionConfig.get();
        if (cfg.sound) play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f);

        say(line("Auction ended: " + itemName, cfg.chatPrefix));
        if (r.winner == null) {
            say(line("Nobody paid.", cfg.chatInfo));
        } else {
            say(Text.literal("Winner: " + r.winner + " paid " + Money.format(r.winningAmount))
                    .styled(s -> s.withColor(TextColor.fromRgb(cfg.chatWinner)).withBold(true)));
            if (cfg.announceWinner) {
                publicChat(fill(cfg.winnerTemplate, itemName, Money.formatDuration(durationSec),
                        r.winner, Money.format(r.winningAmount)));
            }
        }
    }

    // ---- helpers ----
    private static void play(net.minecraft.sound.SoundEvent sound, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch)));
    }

    private static net.minecraft.text.MutableText line(String s, int rgb) {
        return Text.literal(s).styled(st -> st.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    private static String fill(String template, String item, String time, String winner, String amount) {
        return template
                .replace("{item}", item)
                .replace("{time}", time)
                .replace("{winner}", winner)
                .replace("{amount}", amount);
    }

    /** Message shown only to you (never sent to the server). */
    private static void say(Text text) {
        AuctionConfig cfg = AuctionConfig.get();
        if (!cfg.chatMessages) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(line("[Auction] ", cfg.chatPrefix).append(text), false);
    }

    /** Optional: sends a normal chat message as you. Commands ("/...") are never sent. */
    private static void publicChat(String message) {
        String m = message == null ? "" : message.trim();
        if (m.isEmpty() || m.startsWith("/")) return;
        if (m.length() > 250) m = m.substring(0, 250);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendChatMessage(m);
    }
}
