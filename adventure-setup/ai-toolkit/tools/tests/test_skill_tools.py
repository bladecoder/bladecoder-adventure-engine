"""Discovery opens canonical workflows and never overwrites independently edited entries."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from skill_tools import check_skills, discovery_files, files, sync_skills


class SkillDiscovery(unittest.TestCase):
    def project(self, parent):
        root = parent / 'game'; (root / 'tools').mkdir(parents=True); (root / 'docs').mkdir()
        (root / 'docs/guide.md').write_text('project guide')
        self.skill(root, 'author')
        return root

    def skill(self, root, name):
        folder = root / 'skills' / name; folder.mkdir(parents=True)
        (folder / 'SKILL.md').write_text('---\nname: ' + name + '\ndescription: Author a small verified interaction.\n---\n\nRead [the guide](../../docs/guide.md).\n')

    def test_discovery_symlink_uses_canonical_skills_without_sync(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); link = root / '.agents/skills'
            link.parent.mkdir(parents=True)
            link.symlink_to('../skills', target_is_directory=True)
            self.assertEqual((link / 'author/SKILL.md').read_bytes(), (root / 'skills/author/SKILL.md').read_bytes())
            self.assertFalse(any(row['status'] == 'error' for row in check_skills(root)))
            result = sync_skills(root)
            self.assertTrue(result['linked']); self.assertEqual(result['changed'], [])
            self.assertFalse((root / 'tools/skills-discovery.json').exists())

    def test_discovery_copy_links_to_canonical_skill_without_development_workflow(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); self.skill(root, 'adventure-player')
            canonical = root / 'skills/adventure-player/SKILL.md'
            original = canonical.read_bytes()
            copy = discovery_files(files(root / 'skills', root))['adventure-player/SKILL.md'].decode()
            self.assertEqual(canonical.read_bytes(), original)
            self.assertIn('../../../skills/adventure-player/SKILL.md', copy)
            self.assertNotIn('canonical project workflow', copy)
            self.assertNotIn('before acting', copy)

    def test_discovery_routes_to_existing_canonical_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); sync_skills(root)
            entry = root / '.agents/skills/author/SKILL.md'
            target = (entry.parent / '../../../skills/author/SKILL.md').resolve()
            self.assertEqual(target, root / 'skills/author/SKILL.md')
            self.assertIn('../../../skills/author/SKILL.md', entry.read_text())
            self.assertNotIn('../../docs/guide.md', entry.read_text())
            self.assertFalse(any(c['status'] == 'error' for c in check_skills(root)))

    def test_crlf_and_quoted_metadata_remain_discoverable(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); source = root / 'skills/author/SKILL.md'
            text = source.read_text().replace('name: author', 'name: "author"')
            source.write_bytes(text.replace('\n', '\r\n').encode())
            sync_skills(root)
            self.assertFalse(any(c['status'] == 'error' for c in check_skills(root)))
            self.assertIn('name: "author"', (root / '.agents/skills/author/SKILL.md').read_text())

    def test_dry_run_does_not_write_and_source_metadata_refreshes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); result = sync_skills(root, dry_run=True)
            self.assertTrue(result['changed']); self.assertFalse((root / '.agents').exists())
            sync_skills(root); source = root / 'skills/author/SKILL.md'
            source.write_text(source.read_text().replace('Author a small', 'Author another'))
            self.assertTrue(any(c['id'] == 'skills.discovery' and c['status'] == 'error' for c in check_skills(root)))
            sync_skills(root)
            self.assertIn('Author another', (root / '.agents/skills/author/SKILL.md').read_text())

    def test_independently_edited_entry_is_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); sync_skills(root)
            entry = root / '.agents/skills/author/SKILL.md'; entry.write_text(entry.read_text() + 'local authored note\n')
            before = entry.read_bytes(); manifest = (root / 'tools/skills-discovery.json').read_bytes()
            result = sync_skills(root)
            self.assertEqual(result['conflicts'], ['author/SKILL.md'])
            self.assertEqual(entry.read_bytes(), before)
            self.assertEqual((root / 'tools/skills-discovery.json').read_bytes(), manifest)

    def test_failed_manifest_update_rolls_back_discovery(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); sync_skills(root)
            entry = root / '.agents/skills/author/SKILL.md'; before = entry.read_bytes()
            source = root / 'skills/author/SKILL.md'; source.write_text(source.read_text().replace('small', 'changed'))
            real_replace = Path.replace
            def replace(path, destination):
                if Path(destination) == root / 'tools/skills-discovery.json': raise OSError('injected manifest write failure')
                return real_replace(path, destination)
            with patch.object(Path, 'replace', replace), self.assertRaises(OSError): sync_skills(root)
            self.assertEqual(entry.read_bytes(), before)

    def test_missing_references_and_symlinks_fail_before_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.project(Path(directory)); (root / 'docs/guide.md').unlink()
            with self.assertRaises(ValueError): sync_skills(root)
            self.assertFalse((root / '.agents').exists())
            (root / 'docs/guide.md').write_text('guide')
            outside = Path(directory) / 'outside'; outside.mkdir()
            (root / '.agents').symlink_to(outside, target_is_directory=True)
            with self.assertRaises(ValueError): sync_skills(root)
            self.assertEqual(list(outside.iterdir()), [])


if __name__ == '__main__': unittest.main()
