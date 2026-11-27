# Trickle

A Fabric mod for Minecraft Java **1.21.1** that adds a slow "trickle" currency. Players earn coins just by actively playing, then spend them on loot boxes.

Install the mod on **both the server and every client** (plus Fabric API). It also works in singleplayer and LAN worlds. The server holds every balance and does all the buying and opening, so a modified client can't give itself coins or items.

## How it plays

- Every 5 minutes of active play earns 1 coin. Players who haven't moved or looked around for 3 minutes stop earning. Spectators don't earn.
- Progress toward the next coin is saved, so logging out doesn't reset it.
- A loot box costs 10 coins. Buy one with `/trickle buy [count]` or the **B** key.
- Right-click a loot box to open it. It rolls 2 to 3 items from the `trickle:loot_box` loot table (bread, iron, gold, XP bottles, emeralds, a random enchanted book, rare diamonds and golden apples).
- The top-left corner shows your balance and a small bar toward the next coin. Hide it with F1, or bind "Toggle coin display" in Controls.

## Commands

| Command | Who | What |
|---|---|---|
| `/trickle` | everyone | Show your balance and the loot box price |
| `/trickle buy [count]` | everyone | Buy loot boxes |
| `/trickle give/take/set <players> <amount>` | operators | Change balances |
| `/trickle reload` | operators | Reload `config/trickle.json` |

## Server config (`config/trickle.json`)

```json
{
  "secondsPerPayout": 300,
  "coinsPerPayout": 1,
  "lootBoxCost": 10,
  "afkTimeoutSeconds": 180,
  "maxBalance": 0,
  "announcePayouts": false
}
```

Change the loot with a datapack that overrides `data/trickle/loot_table/loot_box.json`.

Balances are saved in the world folder at `data/trickle.dat`.

## Building

Needs Java 21. Run `./gradlew build`; the mod jar ends up in `build/libs/`. Run `./gradlew runClient` or `./gradlew runServer` to test.
