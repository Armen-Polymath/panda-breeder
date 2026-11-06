# Panda Breeder

Client-only Fabric mod for **Minecraft Java 1.21.11**. Read synchronized panda genetics, calculate exact vanilla offspring probabilities, and plan breeding toward brown pandas. No modified breeding mechanics, custom server packets, or server installation.

## Installation

1. Use Minecraft **1.21.11**, Java **21 or newer**, and Fabric Loader **0.18.4 or newer**.
2. Install **Fabric API 0.141.4+1.21.11**, the tested version.
3. Put `panda-breeder-1.0.0.jar` in the client's `mods` directory. Do not install the `-sources.jar`.
4. Launch the Fabric client and join a world.

Intended for vanilla, Fabric and Paper servers using vanilla panda mechanics. Actual runtime test coverage is documented below.

## Controls and workflow

| Key | Action |
| --- | --- |
| **R** | Register/refresh the panda under the crosshair |
| **P** | Open the assistant |
| **O** | Toggle in-world labels |

Rebind in **Options → Controls → Key Binds → Panda Breeder** to avoid conflicts with other mods.

**Collection:** pandas receive stable IDs such as P01, P02, P03. Re-registering a UUID updates its record. IDs are not reused after unregistering. The scrollable list displays main/hidden genes, brown-copy count, age and availability. Select an ID and **Inspect** to view personality, coordinates, pedigree, UUID and offspring count. **Unregister** only removes local tracking; it never affects the entity.

**Calculator:** choose distinct pandas using the cycle buttons, or use Parent A/B in Collection. View brown, one-brown-copy and zero-brown-copy probabilities, fixed-pair expected attempts, and the complete scrollable distribution of 49 ordered genotypes. Babies and saved unloaded pandas can be evaluated hypothetically.

**Strategy:** recommends loaded adults and reports expected attempts under an adaptive plan that retains useful offspring. New carriers improve future breeding. This minimizes attempts within the documented model, not elapsed time. Check bamboo, cooldown and readiness in game.

**Offspring:** register the baby with R. Select the first parent and click **Parent A**, the second and **Parent B**, then select the baby and **Assign baby**. This records manually confirmed parentage and offspring history. Optional **Auto babies** registers newly seen babies within 16 blocks of a tracked adult. This is a discovery heuristic: unrelated babies can qualify, and proximity never assigns parents. Enable before the baby appears; babies already seen while disabled need manual registration.

**Overlays:** compact labels show ID, abbreviated genes and brown-copy count (e.g. `★ P01 NO/BR 1B`). A green star marks the recommended pair. Gene abbreviations: NO normal, LA lazy, WO worried, PL playful, BR brown, WE weak, AG aggressive. Labels are limited to 32 blocks, respect hidden HUD and invisible entities, and include existing custom names. Toggle in Settings or with O. Only per-frame render state changes; entity names and metadata remain untouched.

## Genetics and uncertainty

Both main and hidden genes are synchronized to ordinary vanilla 1.21.11 clients. The mod reads `PandaEntity.getMainGene()` and `getHiddenGene()` directly. Default-normal values are valid. The engine reproduces random parent order, equal inheritance from each parent's two genes, independent 1/32 mutation at each locus and vanilla's weighted replacement distribution. Brown and weak are recessive.

The pure engine accepts normalized uncertain genotype distributions. Records distinguish synchronized values from inferred distributions; assigning parents updates an uncertain child's distribution. The wild prior is an assumption, not an observation. Standard gameplay uses actual synchronized genes; the optimizer excludes uncertain records. Falsified metadata and modified server breeding rules cannot be detected automatically. Independent parental beliefs do not represent pedigree correlations.

See [verified genetics](docs/GENETICS.md) and [optimizer derivation](docs/OPTIMIZER.md).

## Persistence and world identity

Local JSON files live in `config/pandabreeder/`, named by a SHA-256 hash of server address or local save path, dimension and selected world profile. Records include genes/uncertainty, location, age, parentage, offspring history and preferences. The selected profile is saved separately.

Writes use a temporary file, atomic replacement where supported and a `.bak` copy of the previous save. Invalid schemas, duplicate IDs, invalid probabilities, cyclic pedigrees or corrupt JSON prevent loading; the original is preserved and saving is disabled for that collection. Check the Minecraft log and restore a valid backup while the client is closed.

Disconnecting clears active records. Reconnecting loads the appropriate collection. Unloaded entities remain saved but are excluded from recommendations; age and death observations may be stale. A vanilla client cannot reliably distinguish world resets or proxy subworlds at the same address. **Choose a new World profile in Settings for these worlds**, and switch back when returning. Changing the address or dimension also selects a different collection.

## Build and tests

Set `JAVA_HOME` to JDK 21 or newer. The wrapper is included; global Gradle is unnecessary.

```powershell
# Windows
.\gradlew.bat --no-daemon build
.\gradlew.bat --no-daemon runClientGameTest
```

```sh
# Linux/macOS
sh gradlew --no-daemon build
sh gradlew --no-daemon runClientGameTest
```

Output: `build/libs/panda-breeder-1.0.0.jar` and a sources JAR. Unit report: `build/reports/tests/test/index.html`. Integration tests require a working graphics environment and launch a temporary singleplayer world. Test fixtures are a separate source set, **not packaged in the release mod**. Only test fixtures write server genes.

## Validation and limitations

- **Build and unit tests passed** on JDK 25 targeting Java 21. Tests cover brown×brown, carrier×carrier, carrier×non-carrier, mutation-only outcomes, distributions, uncertainty, adaptive strategy, parentage, persistence, backup, corruption and isolation.
- **Singleplayer client integration passed:** all 16 vanilla mutation-selector outcomes; 100,000 seeded vanilla inheritance samples against the engine; both synchronized genes including normal defaults; duplicate registration; GUI parent assignment; metadata refresh; save/reload; profile isolation; disconnect cleanup. Screenshots are captured for visual review.
- **Dedicated multiplayer/Paper runtime testing was not performed**, at the project owner's request. Compatibility follows from standard metadata and no server dependencies, rather than a completed server test matrix.
- Growth, cooldown, bamboo, travel, survival, capacity and modified server mechanics are excluded from optimizer costs. Exactness applies only within its stated assumptions.
- Automatic discovery cannot prove a birth or parentage. Parentage requires explicit confirmation. Unknown parent beliefs may be correlated; the calculator assumes independence.
- Interface language is English. Long text can be truncated at large GUI scales; complete model explanations are in the linked documents.

## Source layout

`Genetics` and `Optimizer` are independent of Minecraft. `CollectionData` manages records/pedigrees. `PandaStore` validates and persists them. `PandaBreederClient` handles entities, lifecycle and keys. `BreedingScreen` is the native GUI. `PandaLabelMixin` changes label render state only.

Development milestones are separate conventional commits on `main`, pushed after their build/tests. No Minecraft assets, credentials, generated build output or IDE caches are committed.
