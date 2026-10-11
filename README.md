# Power and Cost

A Minecraft Fabric mod for enchanted gear maintenance and modification cost-power relationships.

## Compatibility

- **Minecraft:** 26.3
- **Fabric Loader:** ≥ 0.19.5
- **Fabric API:** 0.162.0+26.3
- **Java:** ≥ 25

## Build Instructions

From the mod project directory:

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Copy the built `power_and_cost-*.jar` from `build/libs/` into your Minecraft `mods` folder

## Branch Policy

Each supported Minecraft version has its own long-lived branch named after the exact `minecraft_version` in `gradle.properties` (e.g. `1.21.10`). Version branches are the source of truth; `main` is not used.

- **Default branch:** `26.3` (also `26.2` for the prior game release)
- **Feature/fix work:** branch from the target version as `{version}/feature-name`, merge back into the version branch
- **New MC version:** create `{version}` from the prior version branch, bump `gradle.properties`, push to origin
- **Retiring a version:** tag the final release, then archive the branch (do not delete)

```bash
git clone https://github.com/spicato-minecraft/power_and_cost.git
cd power_and_cost
git checkout 26.3
```

## Releases and Modrinth

Merging code does **not** publish to Modrinth. Publishing happens only when a GitHub Release is created.

### Version tags

Release tags use `{minecraft_version}-{mod_version}` (e.g. `1.21.10-1.0.0`), matching `gradle.properties`.

### How to publish

1. Merge your changes into the version branch (e.g. `1.21.10`).
2. Bump `mod_version` in `gradle.properties` if shipping new player-facing changes.
3. Add a changelog at `releases/{tag}.md` — user-facing bullets in plain language (see `releases/TEMPLATE.md`).
4. Create a GitHub Release with tag `{minecraft_version}-{mod_version}` targeting the version branch.
5. CI runs `publish.yml`: validates tag + changelog, runs `./gradlew build`, uploads to Modrinth.

```bash
gh release create 1.21.10-1.0.0 \
  --target 1.21.10 \
  --title "1.21.10-1.0.0" \
  --notes-file releases/1.21.10-1.0.0.md
```

Infra-only merges (tests, CI, docs) never need a release unless you intend to ship a new mod version.
