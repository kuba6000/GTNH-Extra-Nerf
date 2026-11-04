"""Describe an exact published JAR and render the user-visible telemetry disclosure."""

import argparse
import hashlib
import json
import re
from pathlib import Path
from zipfile import ZipFile

NOTICE_MARKER = "<!-- extra-nerf-telemetry -->"
NOTICE = """<!-- extra-nerf-telemetry -->
### Telemetry

Extra Nerf telemetry is opt-in. An undecided world sends no statistics or team reports until an
operator (or the owner of a singleplayer world) chooses in a window whether to send nothing,
basic statistics or team tracking; an existing team opt-in or telemetry refusal is carried over.
A dedicated server's console is told how to answer with `extranerftelemetry on` or `off`. The choice is saved in
the world and can be changed at any time with `/extranerftelemetry on|off` or
`/extranerfteam on|off`. Basic statistics and team tracking are mutually exclusive.

Basic statistics include a random ID valid for one server run, the online player count, the
highest available world progression tier, singleplayer status and an optional randomized JAR
proof, about once per hour. No player names, player UUIDs or world coordinates are included.
Reports go to `extranerf.kuba6000.pl`, and contribute to community statistics about
active servers, online players and world progression, and help us understand mod usage.
Team reports also provide those statistics and include a persistent team ID plus the leader's
and online players' Minecraft names and UUIDs. Eligible teams may be listed publicly.
Read our [Privacy & Telemetry Policy](https://extranerf.kuba6000.pl/privacy).

Development environments do not send production reports. See the repository's `docs/TELEMETRY.md`.
"""


def describe(jar_path: Path, tag: str) -> dict:
    if not re.fullmatch(r"[!-~]{1,128}", tag):
        raise ValueError("A nonempty printable ASCII release tag is required")
    if jar_path.name.endswith(("-dev.jar", "-sources.jar", "-api.jar")):
        raise ValueError("Select the runtime JAR, not development/source/API artifacts")
    with ZipFile(jar_path) as jar:
        manifest = jar.read("META-INF/MANIFEST.MF").decode("utf-8").replace("\r\n", "\n")
        manifest = re.sub(r"\n ", "", manifest)
        attributes = dict(line.split(": ", 1) for line in manifest.split("\n\n", 1)[0].splitlines() if ": " in line)
        if attributes.get("ExtraNerf-Telemetry-Release") != tag:
            raise ValueError("The JAR's embedded release tag does not match the GitHub tag")
        metadata = json.loads(jar.read("mcmod.info"))
        mods = metadata.get("modList", []) if isinstance(metadata, dict) else metadata
        matching = [mod for mod in mods if mod.get("modid") == "gtnhextranerf"]
        if len(matching) != 1:
            raise ValueError("Expected one Extra Nerf entry in mcmod.info")
        version = matching[0]["version"]
        if not isinstance(version, str) or not re.fullmatch(r"[!-~]{1,128}", version):
            raise ValueError("Invalid telemetry mod version")
        if version != tag:
            raise ValueError("The mod version must equal the release tag")
    with jar_path.open("rb") as stream:
        digest = hashlib.sha256()
        for chunk in iter(lambda: stream.read(65536), b""):
            digest.update(chunk)
    return {"modVersion": version, "jarSha256": digest.hexdigest(), "artifact": jar_path.name}


def main() -> None:
    parser = argparse.ArgumentParser()
    files = parser.add_mutually_exclusive_group(required=True)
    files.add_argument("--jar", type=Path)
    files.add_argument("--directory", type=Path)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--release-body-json", type=Path)
    parser.add_argument("--notes-output", type=Path)
    args = parser.parse_args()
    candidates = [args.jar] if args.jar else sorted(args.directory.glob("*.jar"))
    candidates = [p for p in candidates if not p.name.endswith(("-dev.jar", "-sources.jar", "-api.jar"))]
    if len(candidates) != 1:
        raise ValueError("Expected exactly one released runtime JAR")
    descriptor = describe(candidates[0], args.tag)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(descriptor, indent=2) + "\n", encoding="utf-8")
    if args.notes_output:
        body = json.loads(args.release_body_json.read_text(encoding="utf-8")).get("body") or ""
        if NOTICE_MARKER not in body:
            body = body.rstrip() + "\n\n" + NOTICE
        args.notes_output.write_text(body, encoding="utf-8")
    print(json.dumps(descriptor, indent=2))


if __name__ == "__main__":
    main()
