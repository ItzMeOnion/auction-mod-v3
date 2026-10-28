package com.onxon.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search every item in the game, pick one, name it, and add it to the auction menu. */
public class AddItemScreen extends BaseScreen {

    private record Entry(Item item, String name, String id, String searchText) {}

    private static List<Entry> everything;

    private static final int TILE = 24;
    private static final int PITCH = 26;

    private final List<Entry> results = new ArrayList<>();
    private TextFieldWidget search;
    private TextFieldWidget nameField;
    private Entry picked;
    private int scroll = 0;

    public AddItemScreen(Screen parent) {
        super(Text.literal("Add item"), parent);
    }

    private static void buildList() {
        if (everything != null) return;
        List<Entry> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            String name = new ItemStack(item).getName().getString();
            String id = AuctionConfig.idOf(item);
            list.add(new Entry(item, name, id, (name + " " + id).toLowerCase(Locale.ROOT)));
        }
        list.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        everything = list;
    }

    @Override
    protected void init() {
        buildList();
        layout();

        search = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Search"));
        search.setMaxLength(40);
        search.setPlaceholder(Text.literal("Search items (e.g. skeleton, shulker, pickaxe)..."));
        search.setChangedListener(s -> {
            scroll = 0;
            refilter(s);
        });

        nameField = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setPlaceholder(Text.literal("Name shown on the card"));
        if (picked != null && nameField.getText().isEmpty()) nameField.setText(picked.name());

        addDrawableChild(search);
        addDrawableChild(nameField);
        setInitialFocus(search);
        refilter(search.getText());
    }

    private void refilter(String query) {
        results.clear();
        String[] tokens = query == null ? new String[0] : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        for (Entry e : everything) {
            boolean ok = true;
            for (String t : tokens) {
                if (!t.isEmpty() && !e.searchText().contains(t)) {
                    ok = false;
                    break;
                }
            }
            if (ok) results.add(e);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        layout();
        drawPanel(ctx, "ADD", "ITEM");
        Ui.rightText(ctx, textRenderer, results.size() + " items", px + pw - 14, py + 9, Ui.MUTED, false);

        int left = px + 14;
        int inner = pw - 28;

        search.setX(left);
        search.setY(py + 22);
        search.setWidth(inner);

        // bottom-up layout
        int buttonsY = py + ph - 12 - 18;
        int nameY = buttonsY - 8 - 16;
        int hoverY = nameY - 13;
        int gridTop = py + 46;
        int gridBottom = hoverY - 4;

        int gridW = inner - 8;
        int cols = Math.max(1, gridW / PITCH);
        int visibleRows = Math.max(1, (gridBottom - gridTop) / PITCH);
        int totalRows = (results.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        Entry hovered = null;
        int first = scroll * cols;
        int last = Math.min(results.size(), (scroll + visibleRows) * cols);
        for (int i = first; i < last; i++) {
            Entry e = results.get(i);
            int cx = left + (i % cols) * PITCH;
            int cy = gridTop + (i / cols - scroll) * PITCH;
            boolean hover = mx >= cx && mx < cx + TILE && my >= cy && my < cy + TILE;
            boolean sel = e == picked;
            if (hover) hovered = e;
            Ui.box(ctx, cx, cy, TILE, TILE, sel ? Ui.CARD_SELECTED : hover ? Ui.CARD_HOVER : Ui.CARD,
                    sel ? Ui.ACCENT : hover ? Ui.MUTED : Ui.BORDER);
            ctx.drawItem(new ItemStack(e.item()), cx + 4, cy + 4);
            hits.add(new Hit(cx, cy, TILE, TILE, () -> choose(e)));
        }
        if (results.isEmpty()) {
            Ui.centerText(ctx, textRenderer, "No items match that search", px + pw / 2, gridTop + 30, Ui.MUTED, false);
        }

        // scrollbar
        if (totalRows > visibleRows) {
            int trackH = gridBottom - gridTop;
            int thumbH = Math.max(12, trackH * visibleRows / totalRows);
            int thumbY = gridTop + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
            int bx = left + inner - 4;
            ctx.fill(bx, gridTop, bx + 4, gridBottom, Ui.CARD);
            ctx.fill(bx, thumbY, bx + 4, thumbY + thumbH, Ui.ACCENT);
        }

        // status line
        Entry shown = hovered != null ? hovered : picked;
        if (shown != null) {
            Ui.text(ctx, textRenderer, shown.name(), left, hoverY, Ui.TEXT, true);
            int nw = Ui.width(textRenderer, shown.name() + "  ", true);
            Ui.text(ctx, textRenderer, shown.id(), left + nw, hoverY, Ui.MUTED, false);
        } else {
            Ui.text(ctx, textRenderer, "Click an item to pick it", left, hoverY, Ui.MUTED, false);
        }

        // name row
        if (picked != null) {
            ctx.drawItem(new ItemStack(picked.item()), left, nameY);
        } else {
            ctx.drawItem(new ItemStack(Items.NAME_TAG), left, nameY);
        }
        nameField.setX(left + 24);
        nameField.setY(nameY);
        nameField.setWidth(inner - 24);

        // buttons
        plainButton(ctx, mx, my, left, buttonsY, 70, 18, "Back", true, this::close);
        boolean canAdd = picked != null && !nameField.getText().isBlank();
        button(ctx, mx, my, px + pw - 14 - 140, buttonsY, 140, 18, "Add to menu",
                canAdd ? 0xFF1E7A4A : Ui.CARD, 0xFF28A164, canAdd ? Ui.WHITE : Ui.MUTED,
                canAdd ? Ui.GREEN : Ui.BORDER, canAdd, this::addPicked);

        super.render(ctx, mx, my, delta);
    }

    private void choose(Entry e) {
        picked = e;
        nameField.setText(e.name());
    }

    private void addPicked() {
        if (picked == null || nameField.getText().isBlank()) return;
        AuctionConfig.get().items.add(new AuctionConfig.ItemEntry(picked.id(), nameField.getText().trim()));
        AuctionConfig.save();
        close();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) Math.signum(verticalAmount);
        return true;
    }
}
