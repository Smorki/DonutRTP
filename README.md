# DonutRTP

Advanced Random Teleport plugin for Paper/Folia 1.21+ with GUI, queue system, and zone support.

## Features

- **GUI-based teleport** - `/rtp` opens an interactive menu for Overworld, Nether, and End
- **1v1 Queue system** - `/rtpqueue` lets players queue for 1v1 fights with random teleport arenas
- **Safe location finding** - Finds safe teleport spots avoiding lava, water, and blacklisted biomes
- **Walk detection** - Cancels teleport if the player moves too far during countdown
- **Countdown system** - Configurable wait time with action bar countdown messages
- **Cooldown system** - Per-player cooldown between teleports
- **RTP Zones** - WorldGuard region-based teleport zones with separate cooldowns
- **Effects on teleport** - Grant potion effects (e.g. Absorption) after teleporting
- **Folia support** - Fully compatible with Folia's region-based scheduling
- **Customizable GUI** - Edit `gui/rtp.yml` and `gui/queue.yml` to change items, names, lore
- **Customizable messages** - All messages configurable via `lang.yml`
- **Custom sounds** - Configure sounds for cooldown, teleporting, success, fail, etc.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rtp` | Open the random teleport GUI | `rtp.use` |
| `/rtpqueue` | Open the 1v1 queue GUI | `rtp.use` |
| `/rtp reload` | Reload all config files | `rtp.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `rtp.use` | Access RTP and queue commands | Everyone |
| `rtp.admin` | Access reload command | Operators |
| `rtp.bypass` | Bypass cooldown restrictions | Operators |

## Dependencies

**Required:**
- Paper 1.21+ (or compatible fork)
- Java 21+

**Optional (soft dependencies):**
- [LuckPerms](https://luckperms.net/) - Player data display in GUI
- [WorldGuard](https://enginehub.org/worldguard) + [WorldEdit](https://enginehub.org/worldedit) - RTP zone features
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - `%donutrtp_zone_countdown_<name>%` placeholder

## Configuration

### config.yml

```yaml
blacklisted-blocks:
  - LAVA
  - WATER

blacklisted-biomes:
  - BEACH

allowed-walk-range: 3        # Blocks player can walk before cancel
wait-time-seconds: 5          # Countdown duration
default-cooldown-seconds: 60  # Cooldown between teleports
pregenerate-location-amount: 10

effects-on-random-teleport:
  '1':
    type: ABSORPTION
    level: 10
    seconds: 10

world-settings:
  overworld-region:
    world: world
    min-x: -50000
    max-x: 50000
    min-z: -50000
    max-z: 50000
  nether-region:
    world: world_nether
    min-x: -50000
    max-x: 50000
    min-z: -50000
    max-z: 50000
  end-region:
    world: world_the_end
    min-x: -50000
    max-x: 50000
    min-z: -50000
    max-z: 50000

rtp-zones:
  '1':
    enabled: true
    zone-region: rtpzone
    zone-world: world
    cooldown-time: 60
    minimum-players: 1
    arena-distance: 10
    glowing-duration: 6
    rtp-worlds:
      - world

sound:
  teleport_cooldown: block.note_block.pling
  teleporting: entity.enderman.teleport
  button_click: ui.button.click
  teleport_success: entity.player.levelup
  teleport_fail: entity.villager.no
  reload: entity.experience_orb.pickup
```

### GUI Customization

Edit `plugins/DonutRtp/gui/rtp.yml` to customize the RTP menu.
Edit `plugins/DonutRtp/gui/queue.yml` to customize the queue menu.

### Messages

Edit `plugins/DonutRtp/lang.yml` to customize all plugin messages.

## Author

- **smorki**
- Discord: `.smorki`

## License

All rights reserved.
