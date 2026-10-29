# Power and Cost — Player Guide

A Minecraft Fabric mod that reworks the anvil economy to make enchanting and repairing feel more balanced and predictable.

---

## Overview

**Power and Cost** changes how the anvil works. Instead of the vanilla system where costs spiral out of control and heavily enchanted items become "Too Expensive," this mod uses a **power level** system. Every item and enchantment has a power value, and those values drive both XP costs and how much durability you get when repairing.

**What you’ll notice in-game:**

- **No more "Too Expensive"** — You can always combine or enchant items, even very powerful ones. Costs cap at a predictable maximum (around 50 levels).
- **Material repair is materials-only** — Repairing with ingots (or other valid materials) costs **no XP levels**. Higher-tier / larger recipes consume more materials per repair step.
- **Heavily enchanted gear repairs less** — The more enchantments you have, the less durability each repair restores. Endgame gear is still repairable, but you’ll use more materials.
- **Early enchants stay cheap** — Adding a few enchantments to a new item costs only a few levels. Costs ramp up as your item gets more powerful.
- **Table XP follows power** — Bookshelves still decide which enchantments appear. The levels you pay scale with the item’s material power plus the power of that button’s offers (same curve as anvil book-apply). Lapis stays 1 / 2 / 3.

---

## How It Works

### Power Level

Every item has a **power level** made from two parts:

1. **Material power** — Based on the item’s tier (wood, stone, iron, diamond, netherite, etc.).
2. **Enchantment power** — Each enchantment adds power based on its type and level.

Higher power means higher XP costs for enchanting/combining (anvil and table), and lower repair effectiveness for durability.

---

## Enchanting Table

The table still works like vanilla for **what** you can roll: bookshelf count, item enchantability, and treasure rules are unchanged. You still need lapis (1, 2, or 3) and enough player levels to click a button.

**What changes:** the XP level cost on each of the three buttons is no longer “bookshelf level 1–30.” It uses the same power-level sigmoid as putting a book on an anvil:

| Aspect | Behavior |
|--------|----------|
| **What appears** | Vanilla (bookshelves + item type) |
| **Lapis** | Vanilla (1 / 2 / 3) |
| **XP cost** | Combined power of the item **plus** the enchantments that button would apply |
| **Cost range** | Same as anvil enchanting: about **1 level** (minimum) to **50 levels** (maximum) |

**Cost behavior:**

- **Low-power items** (wood, stone, a regular book) — first rolls stay cheap.
- **High-material items** (diamond, netherite) — the same roll costs more than on wood, because material power counts.
- **Stronger rolls** — a button that would apply more or stronger enchantments costs more than a weak roll on the same item.
- **Books** — a book has no material power, so you mostly pay for the enchantment(s) offered.

You still need enough player levels for that slot’s **vanilla bookshelf power** (the number on the right — a 15-shelf table still unlocks the top row around 30). The **left orb** and the tooltip’s “Enchantment Levels” line are the XP you pay. Lapis stays 1 / 2 / 3. Take the item out and put it back (or wait a moment) after adding shelves so offers refresh. The table does not enchant already-enchanted gear.

---

## Anvil Operations

### Renaming

Renaming an item costs **1 level**, same as vanilla. No changes.

---

### Repairing (Item + Material)

When you repair an item with its repair material (e.g., iron ingots for iron tools):

| Aspect | Behavior |
|--------|----------|
| **XP cost** | **None** (renaming still costs 1 level) |
| **Material cost** | Recipe size **x** sets full-bar efficacy **y = √(3x) − 0.7** (materials worth of durability). Each diamond/ingot restores **1/y** of max durability (fractional — 4 mats on a 4.2-y chestplate leaves it damaged). You spend whole items up to what you offered or **ceil** of what a full fix needs. Enchant scaling applies **after** this. |
| **Durability restored** | Depends on your **enchantments only** — not the material tier |

**Repair effectiveness:**

- **Unenchanted or lightly enchanted** — You get about the same or slightly more durability than vanilla per repair.
- **Moderately enchanted** — Durability per repair starts to drop.
- **Heavily enchanted (god-tier gear)** — Each repair restores about one-third of what vanilla would. You can still repair, but you’ll need more materials.

Recipe size affects **how many materials** one repair step consumes; enchantments affect **how much durability** those materials restore. Material tier (wood vs netherite) does not change the enchantment durability penalty.

---

### Enchanting (Item + Enchanted Book)

When you add an enchantment from a book to an item:

| Aspect | Behavior |
|--------|----------|
| **XP cost** | Based on the **combined power** of the resulting item (material + all enchantments). |
| **Cost range** | Roughly **1 level** (minimum) to **50 levels** (maximum). |

**Cost behavior:**

- **Low-power items** — Adding your first few enchantments costs around 3–5 levels.
- **Mid-power items** — Costs increase as you add more or stronger enchantments.
- **High-power items** — Costs approach the cap of about 50 levels. No more "Too Expensive."

---

### Combining (Item + Same Item)

When you combine two items of the same type (e.g., two diamond swords):

| Aspect | Behavior |
|--------|----------|
| **XP cost** | Same formula as enchanting — based on the **combined power** of the result. |
| **Durability restored** | Same logic as repair — based on the **left item’s enchantments**. |

You get both effects in one operation: enchantments from the right item are merged into the left, and durability is transferred from the right to the left, scaled by the repair factor.

Two items of the same kind can also be combined when the materials differ — two pickaxes, two swords, or two helmets. The result is the higher-tier item, with compatible enchantments from the other item moved onto it. An enchanted iron pickaxe and an unenchanted diamond pickaxe produce the diamond pickaxe carrying those enchantments. Either slot order works. The level cost uses the same power-level formula. If neither item has an enchantment the other can take, there is no result.

---

## Material Power Values (Reference)

Approximate power values by tier:

| Tier | Power |
|------|-------|
| Wood / Leather | 10 |
| Stone / Chainmail | 20 |
| Copper | 25 |
| Gold | 30 |
| Iron | 40 |
| Diamond | 70 |
| Netherite | 100 |

**Special items:** Bow (70), Crossbow (40), Trident (45), Elytra (55), Shield (40), Fishing Rod (20), etc.

---

## Enchantment Power (Reference)

Enchantments add power per level. Higher values mean the enchantment contributes more to costs and repair penalties.

| Enchantment | Power per level |
|-------------|-----------------|
| Sharpness, Power, Fortune, Protection | 14 |
| Smite, Bane of Arthropods, Breach, Density, Impaling, Silk Touch | 12 |
| Efficiency, Looting | 10 |
| Unbreaking, Fire Aspect, Sweeping Edge, Flame, Punch, and most armor/utility enchants | 8 |
| Knockback, Aqua Affinity, Channeling, Wind Burst, Mending, Infinity, Curses, Soul Speed, Swift Sneak | 4 |

---

## Design Philosophy

- **Encourages high-level enchants** — Putting strong enchantments on items early is efficient. Costs scale with total power, so building up gradually is more expensive than going for strong enchants from the start.
- **Discourages "god-tier stacking"** — Combining many low-level enchants into one super-item is costly. The system favors focused, powerful builds.
- **Repair stays viable** — Heavily enchanted gear never becomes unrepairable. You’ll use more materials, but you can always maintain your gear.
- **Predictable caps** — XP costs top out around 50 levels. No more hitting an invisible wall at 40.

---

## Compatibility

- **Minecraft:** 1.21.10
- **Fabric Loader:** ≥ 0.18.4
- **Fabric API:** Required

---

## Summary

| Operation | XP Cost | Material Cost | Durability Effect |
|-----------|---------|---------------|-------------------|
| Enchanting table | 3–50 levels (item + offer power) | Lapis 1 / 2 / 3 | — |
| Rename | 1 level | — | — |
| Repair | 0 levels | Scaled by recipe size | Scaled by enchantments (less for heavily enchanted) |
| Enchant (book) | 3–50 levels | — | — |
| Combine (same item) | 3–50 levels | — | Scaled by left item’s enchantments |
