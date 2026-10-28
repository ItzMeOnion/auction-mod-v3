# Onxon's Auction Mod

Client-side Fabric mod for **Minecraft 1.21.8** (DonutSMP). Version 1.1.

## Auctions
- Press **K** (rebindable under Controls) to open the menu.
- Pick an item card, pick **30 sec**, **1 min** or **Custom** time (`45`, `2m`, `1m30s`, `1:30`), then **Start Auction**.
- The timer and a live leaderboard show on screen and track every `Name paid you $X` message.
- Ranking uses each player's **highest single payment** (payments are never added together).
  1st / 2nd / 3rd are gold / silver / bronze by default.
- When the timer ends you get the winner and amount on screen and in your chat (only you see it).
- **History** tab: recent auctions, what people paid, payments / players / total.

## Edit layout (new in 1.1)
Press **Edit layout** on the New Auction tab:
- Click an item to pick it up, then click the spot it should go to.
- Red **x** removes an item. **+ Add item** opens a search over every item in the game; pick one,
  name it, and it joins the menu with its real picture. Add the same item twice with different names if you like.
- **- / +** changes how many cards fit per row. **Reset items** restores the defaults.

## Settings (new in 1.1)
Press **Settings** in the menu. Everything saves automatically.
- **Menu / Leaderboard / Chat** tabs: pick any colour from a palette or type a hex code, with a live preview.
  Covers the panel, cards, borders, text, 1st/2nd/3rd place, the timer colours, and the chat message colours.
- **General**: show or hide the on-screen timer, timer position, how long the result panel stays,
  chat messages on/off, end sound, countdown beeps, close the menu on start.
- **Announce** (off by default): optionally send the start / winner to public chat using your own message templates.

Files are saved in your Minecraft `config` folder:
`onxons-auction-config.json` (settings + item layout) and `onxons-auction-history.json`.

## Build
Needs Java 21. Either let GitHub Actions build it (see `.github/workflows/build.yml`,
download the jar from the run's **Artifacts**), or run `gradle build` locally.
The jar ends up in `build/libs/`.

Requires Fabric Loader 0.16.14+ and Fabric API for 1.21.8.
