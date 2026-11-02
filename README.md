# GTNH Extra Nerf

GTNH Extra Nerf is a fixed-ruleset difficulty add-on for GregTech: New
Horizons on Minecraft 1.7.10. The mod is built for the pinned full GTNH environment.

It just nerfs stuff.

Steam Input Hatches hold 640,000 L to reduce power failures on high-demand recipes.

Supported pack release: **GT New Horizons 2.9.0** (see
[compatibility](docs/COMPATIBILITY.md)). Release JARs are published on the
[GitHub releases page](https://github.com/kuba6000/GTNH-Extra-Nerf/releases).

## Telemetry and team tracking

Telemetry is enabled by default outside development environments. The logical server
reports a random ID valid for one server run, online player count, highest available
world progression tier, singleplayer status and an optional randomized JAR proof
about once per hour.
We'd appreciate you keeping telemetry enabled. It is used only for community
statistics about active servers, online players, and world progression.
The owner can disable reporting with
`/extranerftelemetry off` or `telemetry.enabled=false` in
`config/gtnhextranerf.cfg`. See the
[Privacy & Telemetry Policy](https://extranerf.kuba6000.pl/privacy) and
[telemetry details and testing](docs/TELEMETRY.md).

Team tracking is a separate feature that is off until an operator runs
`/extranerfteam on`; see [team tracking](docs/TEAM_TRACKING.md). The mod also checks
for newer releases ([update check](docs/UPDATE_CHECK.md)).

## Documentation

- [Project overview](docs/OVERVIEW.md)
- [Complete gameplay change catalog](docs/NERFS.md)
- [Progression certificates](docs/PROGRESSION.md) and [certification exams](docs/CERTIFICATION_EXAM.md)
- [Fictional component tiers and registry](docs/COMPONENTS.md)
- [Compatibility and supported environment](docs/COMPATIBILITY.md)
- [Building and running](docs/BUILDING.md)
- [Telemetry and opt-out](docs/TELEMETRY.md)
- [Update check](docs/UPDATE_CHECK.md)
- [Opt-in team tracking](docs/TEAM_TRACKING.md)
- [Technical overview](docs/TECHNICAL_OVERVIEW.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Agent and maintainer documentation](AI/README.md)

[`docs/NERFS.md`](docs/NERFS.md) is the list of every active gameplay change.

## Build and test

Clone the repository with its GTNHGradle submodule, then run the Gradle checks:

```bash
git clone --recurse-submodules https://github.com/kuba6000/GTNH-Extra-Nerf.git
cd GTNH-Extra-Nerf
./gradlew check
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat check
.\gradlew.bat build
```

Build artifacts are written to `build/libs`. See
[`docs/BUILDING.md`](docs/BUILDING.md) for full-pack client/server tasks and
toolchain details.

## Integrity model

Recipe patches identify exact upstream recipes and require their expected
cardinality. Typed audit scopes aggregate successful semantic changes from
recipe helpers and audited mixins. In production, a missing, duplicate or
rejected patch produces a fatal `GTNH Extra Nerf integrity check failed`
startup error instead of silently running an incomplete balance ruleset to prevent potentially gamebreaking bugs. In a
deobfuscated development environment, the same failures are logged as warnings
and startup continues so an incomplete local runtime remains usable.

## License

GTNH Extra Nerf is licensed under the GNU Lesser General Public License,
version 3 or later. See [`LICENSE`](LICENSE).
