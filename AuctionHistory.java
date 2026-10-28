package com.onxon.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Saves recent auctions to config/onxons-auction-history.json (newest first). */
public final class AuctionHistory {
    private AuctionHistory() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("onxons_auction_mod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_RECORDS = 100;

    private static List<AuctionRecord> records = new ArrayList<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("onxons-auction-history.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                List<AuctionRecord> loaded = GSON.fromJson(Files.readString(f), new TypeToken<List<AuctionRecord>>() {}.getType());
                if (loaded != null) records = new ArrayList<>(loaded);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not load auction history", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(records));
        } catch (Exception e) {
            LOGGER.warn("Could not save auction history", e);
        }
    }

    public static void add(AuctionRecord record) {
        records.add(0, record);
        while (records.size() > MAX_RECORDS) records.remove(records.size() - 1);
        save();
    }

    public static void clear() {
        records.clear();
        save();
    }

    public static List<AuctionRecord> all() {
        return Collections.unmodifiableList(records);
    }
}
