package com.onxon.auction;

import java.util.ArrayList;
import java.util.List;

/** One finished auction, saved to disk as JSON. */
public class AuctionRecord {

    public static class TopEntry {
        public String player;
        public long amount;

        public TopEntry() {}

        public TopEntry(String player, long amount) {
            this.player = player;
            this.amount = amount;
        }
    }

    public String item;
    public String itemId;         // e.g. minecraft:elytra (null for custom items / old saves)
    public long durationSeconds;
    public long endedAt;          // epoch millis
    public String winner;         // null if nobody paid
    public long winningAmount;
    public int payments;          // total number of payments received
    public int players;           // number of different people who paid
    public long totalPaid;        // sum of every payment received
    public List<TopEntry> top = new ArrayList<>();
}
