"""Build the pinned Horizon-QA PR and run its portable client supervisor."""

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import uuid
import zipfile


REVISION = "ad5adb0f8ccede91de4b5da2db50fc0534a4250c"
REPOSITORY = "https://github.com/GTNewHorizons/Horizon-QA.git"


def run(command, cwd, timeout=600):
    subprocess.run(command, cwd=cwd, check=True, timeout=timeout)


def prepare_framework(source=None):
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    cache = gradle_home / "caches" / "extra-nerf-horizonqa" / REVISION
    marker = cache / "framework.json"
    if marker.is_file():
        metadata = json.loads(marker.read_text(encoding="utf-8"))
        if metadata["revision"] != REVISION:
            raise RuntimeError("Horizon-QA cache revision differs from the pinned PR")
        for name in ("horizonqa.jar", "horizonqa-dev.jar", "tools/scripts/client-tests/build.gradle"):
            if not (cache / name).is_file():
                raise RuntimeError("Incomplete Horizon-QA cache: " + str(cache / name))
        return cache
    cache.mkdir(parents=True, exist_ok=True)
    if source is None:
        source = cache / "source"
        if not source.exists():
            run(["git", "clone", "--filter=blob:none", "--single-branch", "--branch",
                 "fullpack-client-testing", REPOSITORY, str(source)], cache)
        run(["git", "checkout", "--detach", REVISION], source)
    source = source.resolve()
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    if revision != REVISION:
        raise RuntimeError("Horizon-QA source must be at PR revision " + REVISION)
    if subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=no"], cwd=source, text=True).strip():
        raise RuntimeError("Horizon-QA source contains tracked changes")
    wrapper = str(source / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    run([wrapper, ":jar", ":reobfJar", ":hqaTools", "--console=plain", "--no-daemon"], source)
    libraries = source / "build" / "libs"
    artifacts = {}
    for destination, pattern in (("horizonqa-dev.jar", "*-dev.jar"), ("tools.zip", "*-tools.zip")):
        artifacts[destination] = max(libraries.glob(pattern), key=lambda path: path.stat().st_mtime)
    artifacts["horizonqa.jar"] = max(
        (path for path in libraries.glob("*.jar") if not path.name.endswith(("-dev.jar", "-sources.jar", "-javadoc.jar"))),
        key=lambda path: path.stat().st_mtime,
    )
    for destination, artifact in artifacts.items():
        shutil.copy2(artifact, cache / destination)
    with zipfile.ZipFile(cache / "tools.zip") as archive:
        for entry in archive.infolist():
            target = (cache / "tools" / entry.filename).resolve()
            if not target.is_relative_to((cache / "tools").resolve()):
                raise RuntimeError("Unsafe path in Horizon-QA tools archive")
        archive.extractall(cache / "tools")
    marker.write_text(json.dumps({"revision": REVISION}, indent=2) + "\n", encoding="utf-8")
    return cache


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tests")
    parser.add_argument("--client-task", choices=(":runFullPack", ":runClient"), default=":runFullPack")
    parser.add_argument("--framework-source", type=Path)
    parser.add_argument("--report-dir", type=Path)
    parser.add_argument("--timeout-seconds", type=int, default=1800)
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    if args.timeout_seconds <= 0:
        parser.error('--timeout-seconds must be positive')
    root = Path(__file__).resolve().parents[2]
    report = (args.report_dir or root / "build" / "horizonqa" / str(uuid.uuid4())).resolve()
    if not args.prepare_only and report.exists():
        raise RuntimeError("Report directory must be new: " + str(report))
    framework = prepare_framework(args.framework_source)
    if args.prepare_only:
        print(framework)
        return 0
    print("Horizon-QA reports: " + str(report), flush=True)
    wrapper = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    selector = args.tests or ("gtnhextranerfdevtests" if args.client_task == ":runClient" else "gtnhextranerftests")
    command = [wrapper, "-p", str(framework / "tools/scripts/client-tests"), "runClientTests",
               "--project-root", str(root), "--client-task", args.client_task, "--tests", selector,
               "--report-dir", str(report), "--timeout-seconds", str(args.timeout_seconds),
               "--python", sys.executable,
               "--gradle-argument=-PhorizonQaIntegration=true",
               "--gradle-argument=-PhorizonQaHome=" + str(framework), "--console=plain", "--no-daemon"]
    completed = subprocess.run(command, cwd=root, timeout=args.timeout_seconds + 120, check=False)
    result = report / "launch-result.json"
    if not result.is_file():
        print("Missing Horizon-QA launcher result: " + str(result), file=sys.stderr)
        return 2
    code = int(json.loads(result.read_text(encoding="utf-8"))["exitCode"])
    if code == 0 and completed.returncode != 0:
        raise RuntimeError("The supervisor build failed despite a successful launch result")
    return code


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as failure:
        print("Horizon-QA infrastructure failure: " + str(failure), file=sys.stderr)
        sys.exit(2)
