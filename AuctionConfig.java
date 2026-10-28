package com.onxon.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Everything the player can customise. Saved to config/onxons-auction-config.json. */
public class AuctionConfig {

    /** One item card in the menu. */
    public static class ItemEntry {
        public String id;
        public String name;
        private transient Item cached;

        public ItemEntry() {}

        public ItemEntry(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Item item() {
            if (cached == null) cached = resolve(id);
            return cached;
        }
    }

    // ---- colours (RGB, no alpha) ----
    public int accent = 0xFF6FAE;
    public int panel = 0x101015;
    public int card = 0x1E1E29;
    public int border = 0x34344A;
    public int text = 0xFFFFFF;
    public int muted = 0x9A9AB0;

    public int rank1 = 0xFFD54A;
    public int rank2 = 0xC8CCD4;
    public int rank3 = 0xCD7F32;
    public int rankOther = 0xE0E0E8;
    public int timerGood = 0x3DDC84;
    public int timerWarn = 0xFFD54A;
    public int timerLow = 0xFF5555;

    public int chatPrefix = 0xFF6FAE;
    public int chatText = 0xFFFFFF;
    public int chatWinner = 0xFFD54A;
    public int chatInfo = 0xAAAAAA;

    // ---- behaviour ----
    public boolean showHud = true;
    public int hudCorner = 0; // 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right
    public int resultSeconds = 30;
    public boolean chatMessages = true;
    public boolean sound = true;
    public boolean countdownBeeps = true;
    public boolean closeOnStart = true;

    // ---- public chat announcements (off by default) ----
    public boolean announceStart = false;
    public boolean announceWinner = false;
    public String startTemplate = "Auction: {item} - pay me to bid! Ends in {time}.";
    public String winnerTemplate = "{winner} won {item} with {amount}! GG";

    // ---- layout ----
    public int columns = 5;
    public List<ItemEntry> items = defaultItems();

    // ------------------------------------------------------------------ static side

    private static final Logger LOGGER = LoggerFactory.getLogger("onxons_auction_mod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AuctionConfig instance = new AuctionConfig();

    public static AuctionConfig get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("onxons-auction-config.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                AuctionConfig loaded = GSON.fromJson(Files.readString(f), AuctionConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    if (instance.items == null) instance.items = defaultItems();
                    instance.columns = Math.max(3, Math.min(7, instance.columns));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not load auction config", e);
        }
        Ui.refresh();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(instance));
        } catch (Exception e) {
            LOGGER.warn("Could not save auction config", e);
        }
    }

    public static List<ItemEntry> defaultItems() {
        List<ItemEntry> l = new ArrayList<>();
        l.add(new ItemEntry("minecraft:elytra", "Elytra"));
        l.add(new ItemEntry("minecraft:spawner", "Spawner"));
        l.add(new ItemEntry("minecraft:netherite_block", "Netherite Block"));
        l.add(new ItemEntry("minecraft:netherite_ingot", "Netherite Ingot"));
        l.add(new ItemEntry("minecraft:ancient_debris", "Ancient Debris"));
        l.add(new ItemEntry("minecraft:netherite_upgrade_smithing_template", "Netherite Upgrade"));
        l.add(new ItemEntry("minecraft:diamond_block", "Diamond Block"));
        l.add(new ItemEntry("minecraft:enchanted_golden_apple", "God Apple"));
        l.add(new ItemEntry("minecraft:beacon", "Beacon"));
        l.add(new ItemEntry("minecraft:totem_of_undying", "Totem of Undying"));
        l.add(new ItemEntry("minecraft:mace", "Mace"));
        l.add(new ItemEntry("minecraft:trident", "Trident"));
        l.add(new ItemEntry("minecraft:dragon_egg", "Dragon Egg"));
        l.add(new ItemEntry("minecraft:shulker_box", "Shulker Box"));
        return l;
    }

    public static Item resolve(String id) {
        Identifier ident = id == null ? null : Identifier.tryParse(id);
        if (ident == null) return Items.NAME_TAG;
        Item item = Registries.ITEM.get(ident);
        return item == Items.AIR ? Items.BARRIER : item;
    }

    public static String idOf(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    /** Icon for a finished auction: by saved item id, else by card name, else a name tag. */
    public static Item iconOf(String id, String name) {
        if (id != null) return resolve(id);
        if (name != null) {
            for (ItemEntry e : instance.items) {
                if (e.name.equalsIgnoreCase(name)) return e.item();
            }
        }
        return Items.NAME_TAG;
    }

    /** Copy every colour from the defaults (does not touch items or toggles). */
    public static void resetColours() {
        AuctionConfig d = new AuctionConfig();
        AuctionConfig c = instance;
        c.accent = d.accent; c.panel = d.panel; c.card = d.card; c.border = d.border;
        c.text = d.text; c.muted = d.muted;
        c.rank1 = d.rank1; c.rank2 = d.rank2; c.rank3 = d.rank3; c.rankOther = d.rankOther;
        c.timerGood = d.timerGood; c.timerWarn = d.timerWarn; c.timerLow = d.timerLow;
        c.chatPrefix = d.chatPrefix; c.chatText = d.chatText; c.chatWinner = d.chatWinner; c.chatInfo = d.chatInfo;
        Ui.refresh();
    }
}
