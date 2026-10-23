"""Malformed project catalogues fail before starting processes or authoring tools."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import load_project


class ProjectDeclarations(unittest.TestCase):
    def load(self, data):
        with tempfile.TemporaryDirectory() as folder:
            Path(folder, "adventure.json").write_text(json.dumps(data), encoding="utf-8")
            return load_project(folder)

    def test_invalid_declarations(self):
        invalid = [
            {"tests": {"suites": []}}, {"tests": []},
            {"tests": {"suites": {"smoke": {"scenario": "../outside.json"}}}},
            {"tests": {"suites": {"bad/name": {"scenario": "smoke.json"}}}},
            {"tests": {"suites": {"smoke": {"scenario": "smoke.json", "engine_replay": 1}}}},
            {"tests": {"suites": {"smoke": {"scenario": "smoke.json", "typo": True}}}},
            {"capabilities": {"ink": "false"}}, {"capabilities": {"ink": True}},
            {"capabilities": {"ink": False}, "ink": {"source": "main.ink", "output": "main.json"}},
            {"art_storage": "drive-personal"}, {"model_contract": "../outside.json"},
            {"tests": {"profiles": []}},
            {"tests": {"profiles": {"all": []}}},
            {"tests": {"profiles": {"fast": [{"id": "../escape", "kind": "tests"}]}}},
            {"tests": {"profiles": {"fast": [{"id": "recursive", "kind": "cli", "value": "qa"}]}}},
            {"tests": {"profiles": {"fast": [{"id": "missing", "kind": "suite", "value": "missing"}]}}},
            {"tests": {"profiles": {"fast": [{"id": "bad", "kind": "trace", "value": {"reference": "absent", "candidate": "absent"}}]}}},
        ]
        for extra in invalid:
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                self.load({"schema_version": 1, **extra})

    def test_valid_neutral_catalogue(self):
        data = {"schema_version": 1, "capabilities": {"ink": False}, "art_storage": "external",
                "tests": {"suites": {"smoke": {"scenario": "tools/runtime-scenarios/smoke.json", "engine_replay": True}}}}
        self.assertEqual(data, self.load(data))

    def test_duplicate_keys_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            Path(folder, "adventure.json").write_text('{"schema_version":1,"tests":{},"tests":{}}')
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                load_project(folder)


if __name__ == "__main__":
    unittest.main()
