"""Behavioral checks for the read-only automation foundation."""

import contextlib
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.dont_write_bytecode = True
SCRIPT = Path(__file__).resolve().parents[1] / "project.py"
sys.path.insert(0, str(SCRIPT.parent))
spec = importlib.util.spec_from_file_location("project_tools", SCRIPT)
project = importlib.util.module_from_spec(spec)
spec.loader.exec_module(project)


class ProjectToolsTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="adventure-tools-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "project with spaces"
        self.root.mkdir()
        self.write("assets/model/world", '{"width":1920,"height":1080,"initChapter":"00","verbs":{}}')
        self.write("assets/model/00.chapter.json", '{"scenes": {}}')
        self.write("adventure.json", json.dumps({"schema_version":1,"art_storage":"external","ink":{"source":"raw/ink/00.ink","output":"assets/model/00.ink.json"}}))
        self.write("gradle.properties", "gdxVersion=1.14.2\n")
        self.write("gradlew", "not executed")
        self.write("gradle/wrapper/gradle-wrapper.jar", "not executed")
        self.write("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=unused")
        self.write("ai-toolkit/skills/example/SKILL.md", "---\nname: example\ndescription: Example\n---\n")
        (self.root / ".agents").mkdir()
        (self.root / ".agents/skills").symlink_to("../ai-toolkit/skills", target_is_directory=True)
        self.write("ai-toolkit/tools/project.py", SCRIPT.read_text())
        for helper in ("skill_tools.py", "model_contract.py", "project_config.py", "art_sources.py", "asset_manifest.py", "chapter_validate.py", "model_json.py", "ink_tools.py", "ink_links.py", "engine_tests.py", "art_pipeline.py", "atlas_tools.py"):
            self.write("ai-toolkit/tools/" + helper, (SCRIPT.parent / helper).read_text())

        shutil.copytree(SCRIPT.parent / "model-contract", self.root / "ai-toolkit/tools/model-contract")

    def write(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def fake_tool(self, name, body):
        path = self.write("fake tools/" + name, f"#!{sys.executable}\n{body}\n")
        path.chmod(0o755)
        return path

    def cli(self, *args, cwd=None, env=None):
        environment = {k: v for k, v in os.environ.items() if not k.startswith("ADVENTURE_")}
        if env:
            environment.update(env)
        return subprocess.run([sys.executable, "-B", str(self.root / "ai-toolkit/tools/project.py"), *args],
                              cwd=cwd or self.root, env=environment, capture_output=True, text=True, timeout=15)

    def snapshot(self):
        return {p.relative_to(self.root).as_posix(): (p.stat().st_mode, hashlib.sha256(p.read_bytes()).hexdigest())
                for p in self.root.rglob("*") if p.is_file()}

    def test_commands_work_from_another_directory_without_writes(self):
        self.write("raw/images/pack.json", "{pot: false,}")
        self.write("assets/ui/ui.json", "{Color: {white: {r: 1}}}")
        before = self.snapshot()
        for command in ("doctor", "inventory", "validate"):
            options = ["--profile", "foundation"] if command != "inventory" else []
            result = self.cli(command, "--json", *options, cwd=self.temporary.name)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            data = json.loads(result.stdout)
            self.assertEqual(data["project_root"], str(self.root))
            self.assertEqual(data["summary"]["error"], 0)
            self.assertEqual(result.stderr, "")
        self.assertEqual(before, self.snapshot())
        self.assertFalse(list(self.root.rglob("__pycache__")))

    def test_invalid_model_json_fails_and_reports_location(self):
        self.write("assets/model/00.chapter.json", '{"scenes":')
        result = self.cli("validate", "--json")
        self.assertEqual(result.returncode, 1)
        errors = [c for c in json.loads(result.stdout)["checks"] if c["status"] == "error"]
        self.assertEqual(errors[0]["path"], "assets/model/00.chapter.json")
        self.assertIn("line 1", errors[0]["message"])

    def test_missing_skill_discovery_is_an_error(self):
        (self.root / ".agents/skills").unlink()
        result = self.cli("doctor", "--profile", "foundation", "--json")
        self.assertEqual(result.returncode, 1)
        self.assertTrue(any(c["id"] == "skills.discovery" and c["status"] == "error"
                            for c in json.loads(result.stdout)["checks"]))

    def test_legacy_warnings_fail_only_in_strict_mode(self):
        self.write("raw/images/packall.sh", "./gen.sh example")
        result = self.cli("validate", "--json")
        strict = self.cli("validate", "--strict", "--json")
        self.assertEqual(result.returncode, 0)
        self.assertEqual(strict.returncode, 1)
        self.assertTrue(any(c["id"] == "legacy.packall" for c in json.loads(result.stdout)["checks"]))

    def test_config_precedence_and_root_relative_paths(self):
        self.write("ai-toolkit/tools/local.json", json.dumps({"paths": {"java": "default-java"}}))
        self.write("custom.json", json.dumps({"paths": {"java": "file-java"}}))
        with patch.dict(os.environ, {}, clear=True):
            self.assertEqual(project.load_config(self.root)["java"], "default-java")
            self.assertEqual(project.load_config(self.root, "custom.json")["java"], "file-java")
            with patch.dict(os.environ, {"ADVENTURE_JAVA": "environment-java"}):
                self.assertEqual(project.load_config(self.root, "custom.json")["java"], "environment-java")
            with patch.dict(os.environ, {"ADVENTURE_TOOLS_CONFIG": "custom.json"}):
                self.assertEqual(project.load_config(self.root)["java"], "file-java")
                self.assertEqual(project.load_config(self.root, "ai-toolkit/tools/local.json")["java"], "default-java")
        path = self.fake_tool("inkscape", "print('Inkscape 1.4.3')")
        config = self.write("custom.json", json.dumps({"paths": {
            "inkscape": path.relative_to(self.root).as_posix(),
            "java": "fake tools/absent-java", "gradle_cache": "empty-cache",
        }}))
        with patch.object(project, "ROOT", self.root), patch.dict(os.environ, {"PATH": ""}, clear=True):
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = project.main(["doctor", "--profile", "graphics", "--json", "--config", str(config)])
        self.assertEqual(code, 1)  # Other graphics requirements are intentionally absent.
        check = next(c for c in json.loads(output.getvalue())["checks"] if c["id"] == "tool.inkscape")
        self.assertEqual(check["status"], "ok")
        self.assertEqual(check["version"], "1.4.3")

    def test_bad_config_is_usage_failure_without_leaking_values(self):
        self.write("bad.json", '{"secret": "DO-NOT-ECHO"}')
        result = self.cli("doctor", "--config", "bad.json", "--json")
        self.assertEqual(result.returncode, 2)
        self.assertNotIn("DO-NOT-ECHO", result.stdout + result.stderr)
        self.assertEqual(json.loads(result.stdout)["checks"][0]["id"], "config")
        missing = self.cli("inventory", "--config", "missing.json", "--json")
        self.assertEqual(missing.returncode, 2)
        invalid = self.cli("doctor", "--profile", "not-a-profile")
        self.assertEqual(invalid.returncode, 2)
        self.assertEqual(invalid.stdout, "")

    def test_missing_tool_and_broken_override_do_not_fall_back(self):
        for profile, expected in (("graphics", "error"), ("all", "warning")):
            checks = []
            project.tool_check(checks, "inkscape", {"inkscape": "missing executable"}, self.root,
                               profile, ("graphics",), ("inkscape",), ("--version",), r"Inkscape (\S+)")
            self.assertEqual(checks[0]["status"], expected)
            self.assertIn("ADVENTURE_INKSCAPE", checks[0]["remedy"])

    def test_help_exit_one_is_valid_for_native_ink_and_jar_command_is_supported(self):
        compiler = self.fake_tool("inklecate", "import sys\nprint('Usage: inklecate <options> <ink file>')\nsys.exit(1)")
        checks = []
        found = project.tool_check(checks, "inklecate", {"inklecate": str(compiler)}, self.root,
                                   "ink", ("ink",), (), (), "never-version")
        self.assertEqual(found, compiler)
        self.assertEqual(checks[0]["status"], "ok")
        self.assertIsNone(checks[0]["version"])
        jar = self.write("compiler.jar", "not executed")
        with patch.object(project, "probe", return_value=(1, "Usage: inklecate <options>")) as probe:
            project.tool_check([], "inklecate", {"inklecate": str(jar)}, self.root,
                               "ink", ("ink",), (), (), "never-version", java=Path("/fake/java"))
        self.assertEqual(probe.call_args.args[0], [Path("/fake/java"), "-jar", jar])

    def test_timeouts_are_actionable_and_spine_is_never_launched(self):
        with patch.object(subprocess, "run", side_effect=subprocess.TimeoutExpired("fake", 5)):
            self.assertEqual(project.probe(["fake"], self.root)[0], None)
        launcher = self.fake_tool("Spine", "raise RuntimeError('must not execute')")
        checks = []
        with patch.object(project, "probe", side_effect=AssertionError("Spine must not be queried")):
            project.tool_check(checks, "spine", {"spine": str(launcher)}, self.root,
                               "spine", ("spine",), (), (), "")
        self.assertEqual(checks[0]["status"], "warning")
        self.assertIsNone(checks[0]["version"])

    def test_packing_discovery_chooses_matching_binaries_and_rejects_sources(self):
        cache = self.root / "cache"
        members = {"gdx": "com/badlogic/gdx/Gdx.class", "gdx-tools": "com/badlogic/gdx/tools/texturepacker/TexturePacker.class"}
        for artifact, versions in (("gdx", ("1.10.0", "1.14.2")), ("gdx-tools", ("1.10.0",))):
            for version in versions:
                jar = cache / "com.badlogicgames.gdx" / artifact / version / "hash" / f"{artifact}-{version}.jar"
                jar.parent.mkdir(parents=True)
                with zipfile.ZipFile(jar, "w") as archive:
                    archive.writestr(members[artifact], b"class")
        checks = []
        project.jar_check(self.root, {"gradle_cache": str(cache)}, checks, "graphics")
        self.assertEqual([c["version"] for c in checks if c["id"].endswith("_jar")], ["1.10.0", "1.10.0"])
        self.assertFalse(any(c["status"] == "error" for c in checks))
        source = self.root / "gdx-tools-1.14.2-sources.jar"
        with zipfile.ZipFile(source, "w") as archive:
            archive.writestr("TexturePacker.java", b"source")
        checks = []
        project.jar_check(self.root, {"gdx_tools_jar": str(source), "gradle_cache": str(cache)}, checks, "graphics")
        self.assertTrue(any(c["id"] == "resource.gdx_tools_jar" and c["status"] == "error" for c in checks))

    def test_inventory_reports_actual_source_and_output_paths(self):
        self.write("raw/ink/00.ink", "-> END")
        self.write("assets/model/00.ink.json", "{}")
        self.write("assets/spine/Character.skel", "runtime")
        data = project.inventory(self.root)
        ink = next(p for p in data["pipelines"] if p["id"] == "ink")
        self.assertEqual(ink["sources"], ["raw/ink/00.ink"])
        self.assertEqual(ink["outputs"], ["assets/model/00.ink.json"])
        self.assertTrue(any(c["id"] == "sources.spine" for c in project.inventory_checks(self.root)))

    def test_invalid_soundfont_is_not_reported_as_ready(self):
        self.write("not-a-bank.sf2", "invalid bank")
        checks = []
        with patch.object(project.ctypes.util, "find_library", return_value=None):
            project.audio_resources(self.root, {"soundfont": "not-a-bank.sf2"}, checks, "audio")
        self.assertTrue(all(c["status"] == "error" for c in checks))

    def test_nonstandard_json_numbers_fail_validation(self):
        self.write("assets/model/00.chapter.json", '{"x": NaN}')
        result = self.cli("validate", "--json")
        self.assertEqual(result.returncode, 1)
        self.assertTrue(any(c["id"] == "chapter.syntax" and c["status"] == "error"
                            for c in json.loads(result.stdout)["checks"]))


if __name__ == "__main__":
    unittest.main()
