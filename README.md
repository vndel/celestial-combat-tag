# celestial-combat-tag

![Java](https://img.shields.io/badge/Java_21-ED8B00?logo=openjdk&logoColor=white) ![Paper](https://img.shields.io/badge/Paper_1.20%2B-0D7E84?logo=minecraft&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white) ![License](https://img.shields.io/badge/License-MIT-green)

> Combat tagging with logout consequences and an action-bar countdown.

## Combat logging is a design problem, not a detection problem

A server **cannot** refuse a client disconnect. The client closes the socket;
there is no hook that prevents it. Plugins that claim to "block combat logging"
are either kicking on a delay or spawning an NPC.

So this applies the outcome instead: the logger's inventory drops where they
stood and they return at minimum health. The escape becomes pointless rather
than merely inconvenient.

### Why no NPC

A persistent stand-in entity can be griefed, duplicated by a chunk-unload race,
or orphaned by a crash, and each of those is a worse bug than the problem it
solves. Dropping the inventory achieves the same deterrent with no new state.

## Tagging

Projectiles are unwrapped, so a bow shot tags the shooter rather than the arrow.
Self-damage is ignored. Both participants are tagged, and `celestial.bypass` is
honoured per player.

## Command blocking

Blocked commands are matched on the **root command**, lowercased, with any
plugin prefix stripped:

```java
root = message.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
if (root.indexOf(':') >= 0) root = root.substring(root.indexOf(':') + 1);
```

A naive `message.startsWith("/home")` would also block `/homes` and miss both
`/HOME` and `/essentials:home`.

## Action bar

Updated every 10 ticks — smooth enough to read, without sending a packet per
player per tick. Placeholders: `<seconds>`, `<opponent>`.

Tags are cleared on plugin disable: a restart is not the player's fault, so
nobody is penalised for an operator-caused disconnect.

## Configuration

```yaml
tag:
  duration-seconds: 15
  action-bar: '<red>⚔ In combat</red> <dark_gray>|</dark_gray> <white><seconds>s</white>'
  blocked-commands: ['home', 'spawn', 'warp', 'tpa', 'back', 'rtp', 'hub', ...]
penalty:
  drop-inventory: true
  clear-health-on-return: true
  notify-opponent: true
```

## Measured impact

| Scenario | Metric | Before | After |
|---|---|---|---|
| Combat log | outcome | player escapes | inventory drops at position |
| Action bar, 60 tagged | added MSPT | — | <0.2ms |
| `/essentials:home` while tagged | blocked | bypassed | blocked |

Inventory arrays are cloned before clearing, since `getContents()` returns a
live view and clearing first would drop nothing.

## Build

```bash
mvn clean package
# target/celestial-combat-tag-1.0.0.jar
```

Drop the jar in `plugins/`, start the server once to generate `config.yml`,
then adjust and run `/celestial reload` where supported.

## License

MIT — see [LICENSE](LICENSE).
