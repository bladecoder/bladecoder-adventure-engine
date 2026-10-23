"""External-source and export-contract behavior, using temporary SVG fixtures."""

import copy
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.dont_write_bytecode = True
TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from art_sources import inspect_art
from asset_manifest import validate_manifests
import project

SVG_TEXT = '''<svg xmlns="http://www.w3.org/2000/svg" width="100" height="80" viewBox="0 0 100 80">
<defs><linearGradient id="paint"><stop offset="0" stop-color="red"/></linearGradient></defs>
<g id="background"><rect id="wall" width="100" height="80" fill="url(#paint)"/></g>
<g id="chair"><rect width="10" height="20"/></g><g id="guides"/>
</svg>'''


class ArtToolsTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="adventure-art-")
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.root = self.base / "repo with spaces"
        self.art = self.base / "art with spaces"
        self.linked = self.base / "other project"
        self.root.mkdir()
        (self.root/"adventure.json").write_text('{"schema_version":1,"art_storage":"external","capabilities":{}}')
        self.art.mkdir()
        self.linked.mkdir()
        self.source = self.write(self.art, "backgrounds/office/office.svg", SVG_TEXT)
        self.write(self.root, "tools/asset-settings/svg-transparent-96.json", (TOOLS / "asset-settings/svg-transparent-96.json").read_text())
        self.manifest = {"schema_version": 1, "id": "office", "source": "backgrounds/office/office.svg",
                         "settings": "tools/asset-settings/svg-transparent-96.json", "exports": [
            {"id": "background", "selection": {"include": ["background"], "exclude": ["guides"]},
             "crop": {"mode": "page"}, "output": "raw/images/office/background.png",
             "destination": {"kind": "atlas", "group": "office"}}]}

    def write(self, root, name, text):
        path = root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def validate(self, data=None, config=None, structure_only=False):
        path = self.write(self.root, "tools/asset-manifests/office.json", json.dumps(data if data is not None else self.manifest))
        return validate_manifests(self.root, config if config is not None else {"art_root": str(self.art)}, [str(path)], structure_only)

    def assert_invalid(self, data, fragment):
        checks = self.validate(data)
        self.assertEqual(checks[0]["status"], "error", checks)
        self.assertIn(fragment, checks[0]["message"])

    def snapshot(self, base):
        return {str(p.relative_to(base)): hashlib.sha256(p.read_bytes()).hexdigest() for p in base.rglob("*") if p.is_file()}

    def test_current_sources_relocate_without_recipe_changes_or_writes(self):
        before = self.snapshot(self.art)
        self.assertEqual(self.validate()[0]["status"], "ok")
        other = self.base / "relocated art"
        shutil.copytree(self.art, other)
        self.assertEqual(self.validate(config={"art_root": str(other)})[0]["status"], "ok")
        self.assertEqual(before, self.snapshot(self.art))
        self.assertEqual(before, self.snapshot(other))
        self.assertFalse((self.root / "raw").exists())

    def test_manifest_cli_accepts_explicit_files_from_another_directory(self):
        for helper in ("skill_tools.py", "model_contract.py", "project_config.py", "project.py", "art_sources.py", "asset_manifest.py", "chapter_validate.py", "model_json.py", "ink_tools.py", "ink_links.py", "engine_tests.py", "art_pipeline.py", "atlas_tools.py"):
            self.write(self.root, "tools/" + helper, (TOOLS / helper).read_text())
        manifest = self.write(self.base, "recipe with spaces.json", json.dumps(self.manifest))
        config = self.write(self.base, "local config.json", json.dumps({"paths": {"art_root": str(self.art)}}))
        env = {k: v for k, v in os.environ.items() if not k.startswith("ADVENTURE_")}
        run = subprocess.run([sys.executable, "-B", str(self.root / "tools/project.py"), "manifests", str(manifest),
                              "--config", str(config), "--json"], cwd=self.base, env=env, capture_output=True, text=True, timeout=10)
        self.assertEqual(run.returncode, 0, run.stdout + run.stderr)
        self.assertFalse(run.stderr)
        self.assertEqual(json.loads(run.stdout)["checks"][0]["id"], "manifest.valid")
        self.assertFalse((self.root / "raw").exists())

    def test_config_root_precedence_and_named_linked_locations(self):
        self.write(self.root, "tools/local.json", json.dumps({"paths": {"art_root": "../art with spaces"}, "linked_roots": {"older-game": str(self.linked)}}))
        with patch.dict(os.environ, {}, clear=True):
            config = project.load_config(self.root)
            self.assertEqual(config["art_root"], "../art with spaces")
            self.assertIn("older-game", config["linked_roots"])
            with patch.dict(os.environ, {"ADVENTURE_ART_ROOT": str(self.base / "override")}):
                self.assertEqual(project.load_config(self.root)["art_root"], str(self.base / "override"))

    def test_missing_art_fails_only_when_source_profile_is_requested(self):
        for helper in ("skill_tools.py", "model_contract.py", "project_config.py", "project.py", "art_sources.py", "asset_manifest.py", "chapter_validate.py", "model_json.py", "ink_tools.py", "ink_links.py", "engine_tests.py", "art_pipeline.py", "atlas_tools.py"):
            self.write(self.root, "tools/" + helper, (TOOLS / helper).read_text())
        shutil.copytree(TOOLS / "model-contract", self.root / "tools/model-contract")
        self.write(self.root,"assets/model/00.chapter.json",'{"scenes":{}}')
        for name in ("gradlew", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
            self.write(self.root, name, "fixture")
        self.write(self.root, "assets/model/world", '{"width":1920,"height":1080,"initChapter":"00","verbs":{}}')
        self.write(self.root, "skills/example/SKILL.md", "---\nname: example\ndescription: Example\n---")
        (self.root / ".agents").mkdir()
        (self.root / ".agents/skills").symlink_to("../skills")
        env = {k: v for k, v in os.environ.items() if not k.startswith("ADVENTURE_")}
        env["ADVENTURE_ART_ROOT"] = str(self.base / "missing")
        for args, code in ((["doctor", "--profile", "art"], 1), (["validate"], 0), (["inventory"], 0)):
            run = subprocess.run([sys.executable, "-B", str(self.root / "tools/project.py"), *args, "--json"],
                                 cwd=self.base, env=env, capture_output=True, text=True, timeout=10)
            self.assertEqual(run.returncode, code, run.stdout + run.stderr)
            self.assertFalse(run.stderr)
            self.assertIsInstance(json.loads(run.stdout)["checks"], list)

    def test_links_embedded_images_xml_base_and_explicit_external_roots(self):
        self.write(self.linked, "texture image.png", "input")
        self.source.write_text('''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><g xml:base="../../../other%20project/">
<image xlink:href="texture%20image.png"/></g><image href="data:image/png;base64,AA=="/></svg>''')
        data, checks = inspect_art({"art_root": str(self.art)}, self.root, required=True)
        self.assertTrue(any(c["id"] == "art.link" and c["status"] == "error" for c in checks))
        data, checks = inspect_art({"art_root": str(self.art), "linked_roots": {"older": str(self.linked)}}, self.root, required=True)
        self.assertFalse(any(c["status"] != "ok" for c in checks), checks)
        self.assertEqual(data["svgs"][0]["embedded_images"], 1)
        self.assertEqual(data["svgs"][0]["dependencies"][0]["status"], "ok")
        (self.linked / "texture image.png").unlink()
        data, checks = inspect_art({"art_root": str(self.art), "linked_roots": {"older": str(self.linked)}}, self.root)
        self.assertTrue(any(c.get("dependency", {}).get("status") == "missing" and c["status"] == "warning" for c in checks))

    def test_bad_svg_internal_references_and_unsupported_network_are_reported(self):
        self.source.write_text('''<svg xmlns="http://www.w3.org/2000/svg"><g id="same"/><g id="same"/>
<rect fill="url(#missing)"/><image href="https://example.invalid/image.png"/></svg>''')
        data, checks = inspect_art({"art_root": str(self.art)}, self.root, True)
        self.assertEqual(data["svgs"][0]["duplicate_ids"], ["same"])
        self.assertEqual(data["svgs"][0]["unresolved_ids"], ["missing"])
        self.assertTrue(any(c.get("dependency", {}).get("status") == "unsupported-uri" for c in checks))
        self.source.write_text("not XML")
        self.assertTrue(any(c["id"] == "art.svg-read" for c in inspect_art({"art_root": str(self.art)}, self.root, True)[1]))

    def test_recipe_paths_and_symlink_escapes_are_rejected(self):
        for value in ("/tmp/private.svg", "../outside.svg", "C:/private.svg", "~/.svg", "backgrounds\\office.svg"):
            data = copy.deepcopy(self.manifest)
            data["source"] = value
            self.assertEqual(self.validate(data)[0]["status"], "error")
        (self.art / "escape.svg").symlink_to(self.write(self.linked, "outside.svg", SVG_TEXT))
        data = copy.deepcopy(self.manifest)
        data["source"] = "escape.svg"
        self.assert_invalid(data, "escapes art_root")
        (self.root / "raw").mkdir()
        (self.root / "raw/images").symlink_to(self.linked)
        self.assert_invalid(self.manifest, "escapes the repository")

    def test_missing_ambiguous_and_conflicting_selections(self):
        for selection, message in (({"include": ["absent"], "exclude": []}, "missing SVG IDs"),
                                   ({"include": ["background", "wall"], "exclude": []}, "overlap through ancestry"),
                                   ({"include": ["wall"], "exclude": ["background"]}, "excluded ancestor"),
                                   ({"include": ["background"], "exclude": ["background"]}, "included and excluded"),
                                   ({"include": ["paint"], "exclude": []}, "not a definition")):
            data = copy.deepcopy(self.manifest)
            data["exports"][0]["selection"] = selection
            self.assert_invalid(data, message)

    def test_cross_manifest_output_duplicates_and_shared_settings(self):
        self.assertEqual(self.validate()[0]["status"], "ok")
        other = copy.deepcopy(self.manifest)
        other["id"] = "second"
        self.write(self.root, "tools/asset-manifests/second.json", json.dumps(other))
        checks = validate_manifests(self.root, {"art_root": str(self.art)})
        self.assertTrue(any(c["status"] == "error" and "Duplicate output" in c["message"] for c in checks))
        data = copy.deepcopy(self.manifest)
        data["settings"] = "assets/ui/ui.json"
        self.assert_invalid(data, "tools/asset-settings")

    def test_version_dimensions_opacity_and_output_validation(self):
        cases = []
        for version in (2, True):
            data = copy.deepcopy(self.manifest); data["schema_version"] = version; cases.append(data)
        data = copy.deepcopy(self.manifest); data["exports"][0]["render"] = {"scale": {"mode": "pixels", "width": 0, "height": 10}}; cases.append(data)
        data = copy.deepcopy(self.manifest); data["exports"][0]["render"] = {"background": {"color": "#000000", "opacity": 2}}; cases.append(data)
        data = copy.deepcopy(self.manifest); data["exports"][0]["render"] = {"scale": {"mode": "dpi", "value": 10 ** 400}}; cases.append(data)
        data = copy.deepcopy(self.manifest); data["exports"][0]["output"] = "tools/unsafe.png"; cases.append(data)
        for data in cases:
            self.assertEqual(self.validate(data)[0]["status"], "error")
        self.assertEqual(self.validate(config={}, structure_only=True)[0]["status"], "ok")
        self.assertEqual(self.validate(config={})[0]["status"], "error")

    def test_family_canvas_alignment_locale_and_nine_patch(self):
        data = copy.deepcopy(self.manifest)
        first = data["exports"][0]
        first.update({"family": "chair", "variant": "front", "locale": "es", "reference_point": {"x": 0, "y": 80}})
        second = copy.deepcopy(first)
        second.update({"id": "other", "variant": "left", "output": "raw/images/office/other.png"})
        data["exports"].append(second)
        self.assertEqual(self.validate(data)[0]["status"], "ok")
        second["reference_point"]["x"] = 10
        self.assert_invalid(data, "inconsistent canvas")
        data = copy.deepcopy(self.manifest)
        export = data["exports"][0]
        export["output"] = "raw/images/office/bubble.9.png"
        self.assert_invalid(data, "requires explicit nine_patch")
        export["render"] = {"scale": {"mode": "pixels", "width": 20, "height": 20}}
        export["nine_patch"] = {"mode": "add-border", "split": [2, 2, 2, 2], "padding": [3, 3, 3, 3]}
        self.assertEqual(self.validate(data)[0]["status"], "ok")
        export["nine_patch"]["split"] = [15, 15, 1, 1]
        self.assert_invalid(data, "exceeds the rendered")

    def test_duplicate_frames_and_multi_page_crops_are_rejected(self):
        data = copy.deepcopy(self.manifest)
        data["exports"][0].update({"family": "animation", "frame": 0, "reference_point": {"x": 0, "y": 0}})
        other = copy.deepcopy(data["exports"][0])
        other.update({"id": "next-frame", "output": "raw/images/office/next.png"})
        data["exports"].append(other)
        self.assert_invalid(data, "Duplicate frame")
        self.source.write_text(SVG_TEXT.replace('</svg>', '<g xmlns:i="http://www.inkscape.org/namespaces/inkscape"><i:page/><i:page/></g></svg>'))
        self.assert_invalid(self.manifest, "single-page SVG")

    def test_inspection_does_not_follow_directory_symlinks(self):
        self.write(self.linked, "other.svg", SVG_TEXT)
        (self.art / "linked-directory").symlink_to(self.linked, target_is_directory=True)
        data, checks = inspect_art({"art_root": str(self.art)}, self.root)
        self.assertEqual(data["svg_count"], 1)
        self.assertTrue(any(c["id"] == "art.symlink" for c in checks))

    def test_unresolvable_art_root_does_not_break_structure_only_checks(self):
        loop = self.base / "source-loop"
        loop.symlink_to(loop)
        config = {"art_root": str(loop)}
        data, checks = inspect_art(config, self.root, required=True)
        self.assertIn(data["status"], ("unavailable", "missing"))
        self.assertEqual(checks[0]["status"], "error")
        self.assertEqual(self.validate(config=config, structure_only=True)[0]["status"], "ok")
        self.assertEqual(self.validate(config=config)[0]["status"], "error")


if __name__ == "__main__":
    unittest.main()
