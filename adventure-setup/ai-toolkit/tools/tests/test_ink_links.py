"""Ink link inspection follows the declared entry and bounded include graph."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ink_links import ink_sources, validate_ink_links


class InkLinks(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def test_declared_nondefault_entry(self):
        self.write("adventure.json", json.dumps({"schema_version": 1, "capabilities": {"ink": True},
            "ink": {"source": "story/dialogues/start.ink", "output": "assets/model/main.ink.json"}}))
        self.write("story/dialogues/start.ink", "INCLUDE side.ink\n-> END")
        self.write("story/dialogues/side.ink", "=== side ===\nHello.\n-> END")
        self.write("assets/model/world", "{}")
        with patch("ink_links.Validator"):
            checks = validate_ink_links(self.root)
        self.assertFalse([row for row in checks if row["status"] == "error"], checks)
        self.assertEqual(2, checks[-1]["sources"])

    def test_disabled_ink_does_not_read_models_or_sources(self):
        self.write("adventure.json", '{"schema_version":1,"capabilities":{"ink":false}}')
        with patch("ink_links.Validator", side_effect=AssertionError("should not inspect models")):
            checks = validate_ink_links(self.root)
        self.assertEqual("ink.not-configured", checks[0]["id"])
        self.assertNotEqual("ok", checks[0]["status"])

    def test_missing_include_cycle_and_escape_are_reported(self):
        for content, expected in [("INCLUDE absent.ink", "Cannot read"),
                                  ("INCLUDE start.ink", "Cyclic"),
                                  ("INCLUDE ../outside.ink", "escapes")]:
            with self.subTest(content=content):
                self.write("story/start.ink", content)
                _, errors = ink_sources(self.root, "story/start.ink")
                self.assertTrue(any(expected in row[2] for row in errors), errors)

    def test_entry_symlink_cannot_escape_project(self):
        with tempfile.TemporaryDirectory() as outside:
            (self.root / "story").symlink_to(outside, target_is_directory=True)
            _, errors = ink_sources(self.root, "story/start.ink")
            self.assertIn("escapes", errors[0][2])


if __name__ == "__main__":
    unittest.main()
