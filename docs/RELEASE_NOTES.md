# Panda Breeder 1.0.0

Client-side Fabric assistant for Minecraft Java 1.21.11.

- Verified direct main/hidden gene synchronization.
- Exact ordered genotype calculator with vanilla mutations and explicit uncertainty distributions.
- Adaptive minimum-attempt breeding optimizer under documented growth/cooldown assumptions.
- Persistent UUID registration, locations, age, manual parentage and offspring history.
- Native collection, calculator, strategy and settings screens.
- Compact optional in-world labels and recommended-pair markers.
- Opt-in nearby baby discovery without invented parentage.
- Isolated world profiles, validated saves and backups.

Requires Java 21+, Fabric Loader 0.18.4+, and Fabric API for Minecraft 1.21.11 (tested with 0.141.4+1.21.11). Install the main JAR in the client's mods folder; do not install the sources JAR.

Validation: 14 unit tests and the singleplayer client integration suite passed. Integration checks the vanilla random gene selector, 100,000 seeded inheritance samples, synchronized genes, registration, GUI parentage, label toggling, automatic baby discovery, metadata refresh, persistence and profile isolation.

Known limits: dedicated multiplayer and Paper were not runtime-tested at the owner's request. Optimization minimizes attempts, not elapsed time. Server rule changes and hidden world resets cannot be detected. Use world profiles for resets/proxy worlds. Parentage is manually confirmed; automatic baby discovery is proximity-based. Unknown correlated pedigrees are outside the optimizer model.
