# Trickle

A Fabric mod for Minecraft Java **26.3** that adds a slow "trickle" currency. Players earn coins just by actively playing, then spend them on loot boxes.

Install the mod on **both the server and every client** (plus Fabric API). It also works in singleplayer and LAN worlds. The server holds every balance and does all the buying and opening, so a modified client can't give itself coins or items.

## How it plays

- Every 5 minutes of active play earns 1 coin. Players who haven't moved or looked around for 3 minutes stop earning. Spectators don't earn.
- Progress toward the next coin is saved, so logging out doesn't reset it.
- There are three loot box tiers. Right-click a box to open it.

| Box | Price | What's inside |
|---|---|---|
| Loot Box | 10 coins | 2-3 everyday items: bread, iron, gold, XP bottles, emeralds, a random enchanted book, and a small chance of diamonds or golden apples |
| Rare Loot Box | 50 coins | 3-4 better items (diamonds, emeralds, ender pearls, golden apples, enchanted books) plus one guaranteed enchanted diamond tool or armor piece |
| Epic Loot Box | 150 coins | One jackpot item (netherite ingot, totem of undying, enchanted golden apple, diamond block, elytra, nether star, heart of the sea or trident), one heavily enchanted diamond piece, and 2-3 more high-value items |

- Buy with `/trickle buy [count]` (common), `/trickle buy rare [count]` or `/trickle buy epic [count]`. The **B** key buys one common box. `/trickle shop` lists the prices.
- The top-left corner shows your balance and a small bar toward the next coin. Hide it with F1, or bind "Toggle coin display" in Controls.

## Commands

| Command | Who | What |
|---|---|---|
| `/trickle` | everyone | Show your balance and the loot box prices |
| `/trickle shop` | everyone | Show the loot box prices |
| `/trickle buy [common\|rare\|epic] [count]` | everyone | Buy loot boxes (common if no tier is given) |
| `/trickle give/take/set <players> <amount>` | operators | Change balances |
| `/trickle reload` | operators | Reload `config/trickle.json` |

## Server config (`config/trickle.json`)

```json
{
  "secondsPerPayout": 300,
  "coinsPerPayout": 1,
  "lootBoxCost": 10,
  "rareLootBoxCost": 50,
  "epicLootBoxCost": 150,
  "afkTimeoutSeconds": 180,
  "maxBalance": 0,
  "announcePayouts": false
}
```

Change the loot with a datapack that overrides `data/trickle/loot_table/loot_box.json`, `rare_loot_box.json` or `epic_loot_box.json`.

Balances are saved in the world folder at `data/trickle/accounts.dat`.

## Building

Needs Java 25. Run `./gradlew build`; the mod jar ends up in `build/libs/`.

`./gradlew runGameTest` boots a test server and checks buying and opening every loot box tier, earning while active, and not earning while idle. It accepts the Minecraft EULA for that test server. `./gradlew runClient` and `./gradlew runServer` start a dev client or server.
