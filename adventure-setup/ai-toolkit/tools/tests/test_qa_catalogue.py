"""Project QA rejects absent coverage and retains failures without aborting later tasks."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from qa_tools import run_qa


class CatalogueQA(unittest.TestCase):
    def project(self, path, profiles):
        path.mkdir()
        (path / 'adventure.json').write_text(json.dumps({'schema_version': 1, 'tests': {'profiles': profiles}}))

    def test_absent_profile_is_incomplete_even_when_allowed(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = parent / 'game'; self.project(root, {})
            checks, result = run_qa(root, {}, 'media', parent / 'evidence', allow_unavailable=True)
            self.assertTrue(result['passed'])
            self.assertFalse(result['complete'])
            self.assertEqual(result['tasks'][0]['status'], 'unavailable')
            self.assertEqual(checks[0]['status'], 'warning')
            self.assertTrue((parent / 'evidence/artifacts/artifact-index.json').exists())

    def test_absent_profile_fails_by_default(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = parent / 'game'; self.project(root, {})
            checks, result = run_qa(root, {}, 'all', parent / 'evidence')
            self.assertFalse(result['passed'])
            self.assertFalse(result['complete'])
            self.assertEqual(checks[0]['status'], 'error')

    def test_failed_task_does_not_hide_later_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = parent / 'game'
            self.project(root, {'fast': [{'id': 'first', 'kind': 'tests'}, {'id': 'second', 'kind': 'formats'}]})
            def run(root, config, private, identifier, *args):
                if identifier == 'first': raise ValueError('deliberate task failure')
                return [{'status': 'ok', 'message': 'next task ran'}], {'passed': True}
            with patch('qa_tools.run_task', side_effect=run):
                _, result = run_qa(root, {}, 'fast', parent / 'evidence')
            self.assertFalse(result['passed'])
            self.assertEqual([t['status'] for t in result['tasks']], ['failed', 'passed'])
            self.assertIn('deliberate', (parent / 'evidence/report.md').read_text())


if __name__ == '__main__': unittest.main()
