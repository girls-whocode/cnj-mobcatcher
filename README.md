# CNJMobCatcher

CNJMobCatcher gives your players a clean way to capture, carry, and release mobs without breaking normal server rules.

It is designed for survival-friendly gameplay, staff control, and easy configuration.

## Compatibility

CNJMobCatcher is intended to work from older supported setups through the latest 26.1 series.

Current project details:
- Plugin version: 2.0.1
- API version: 1.21
- Java: 21 (Will work with Java 25)

Recommended server software:
- Paper
- Spigot-compatible servers with matching API support

## What It Adds To Your Server

### Capture Mobs With A Dedicated Item
- Players use a custom catcher item to pick up mobs.
- The item can be renamed and styled to match your server theme.
- Captured data stays on the item, so it moves with the player.

### Choose How Captures Work
- Storage mode: keep captured mobs inside the catcher.
- Egg mode: convert captures into custom egg items.
- Hybrid mode: let permissions decide who gets which behavior.

### Keep Progression Balanced
- Set limited uses or unlimited uses.
- Offer different catcher strengths through admin-given amounts.
- Keep catching meaningful without making it overpowered.

### Protect Server Rules And Spaces
- World-based allow or deny lists for capture and release.
- Optional claim-aware release checks when GriefPrevention is present.
- Bypass permissions for staff moderation.

### Preserve Mob Variety
- Captures retain variant information for supported mobs.
- Example support includes Sheep color, Frog variant, Axolotl variant, and Villager type.

### Better Player Feedback
- Clear action messages for success and failure cases.
- Sound and particle feedback on capture and release.
- Configurable text so your server voice stays consistent.

### Admin-Friendly Controls
- Quick give command for staff.
- Reload command for config updates.
- Recipe can be enabled, disabled, and customized.

## Feature Summary

- Reusable catcher item
- Storage, Egg, and Hybrid capture modes
- Configurable uses system including unlimited mode
- Stored mob counts inside a single catcher
- Variant-aware capture and release for supported entities
- World restrictions for capture and release
- Optional GriefPrevention claim checks
- Entity blacklist support
- Fully configurable crafting recipe
- Centralized configurable messaging
- Startup config validation with safe fallbacks

## Commands

- /mobcatcher give <player> <uses>
- /mobcatcher reload

## Permissions

- cnj.mobcatcher.use
- cnj.mobcatcher.capture
- cnj.mobcatcher.release
- cnj.mobcatcher.admin
- cnj.mobcatcher.bypass.blacklist
- cnj.mobcatcher.bypass.worlds
- cnj.mobcatcher.bypass.claims
- cnj.mobcatcher.mode.storage
- cnj.mobcatcher.mode.egg

## Setup

1. Drop the plugin jar into your plugins folder.
2. Start the server once to generate config files.
3. Adjust config values for your server style and rules.
4. Restart or run the reload command (/mobcatcher reload).

## Build

Requirements:
- Java 21

