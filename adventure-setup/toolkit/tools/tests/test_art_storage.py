"""Art roots are local preferences; initialization never overwrites artist sources."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from art_storage import configure_art_root


class ArtStorage(unittest.TestCase):
    def project(self, parent):
        root = parent / 'game'; (root / 'tools').mkdir(parents=True)
        (root / 'raw/art').mkdir(parents=True)
        (root / 'raw/art/starter.svg').write_text('<svg/>')
        (root / 'tools/recipe.json').write_text('{"source":"starter.svg"}')
        return root

    def test_seed_preserves_recipes_and_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = self.project(parent); destination = parent / 'art with café'
            local = root / 'tools/local.json'
            local.write_text(json.dumps({'paths': {'spine': '/local/spine'}, 'linked_roots': {'textures': '/local/textures'}}))
            recipe = (root / 'tools/recipe.json').read_bytes()
            result = configure_art_root(root, str(destination), seed=True)
            self.assertEqual(result['seeded_files'], ['starter.svg'])
            self.assertEqual((destination / 'starter.svg').read_bytes(), (root / 'raw/art/starter.svg').read_bytes())
            self.assertEqual(json.loads(local.read_text())['paths'], {'spine': '/local/spine', 'art_root': str(destination)})
            self.assertEqual(json.loads(local.read_text())['linked_roots'], {'textures': '/local/textures'})
            self.assertEqual(recipe, (root / 'tools/recipe.json').read_bytes())

    def test_dry_run_changes_nothing(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = self.project(parent); destination = parent / 'art'
            configure_art_root(root, str(destination), seed=True, dry_run=True)
            self.assertFalse(destination.exists())
            self.assertFalse((root / 'tools/local.json').exists())

    def test_occupied_and_symlink_destinations_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = self.project(parent); destination = parent / 'art'; destination.mkdir()
            for target in (destination, parent / 'link'):
                if target.name == 'link': target.symlink_to(destination, target_is_directory=True)
                with self.assertRaises(ValueError): configure_art_root(root, str(target), seed=True)
            self.assertTrue((parent / 'link').is_symlink())
            self.assertFalse((destination / 'starter.svg').exists())
            self.assertFalse((root / 'tools/local.json').exists())

    def test_existing_art_is_only_adopted(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = self.project(parent); destination = parent / 'art'; destination.mkdir()
            artist = destination / 'artist.svg'; artist.write_text('original artist data')
            configure_art_root(root, str(destination))
            self.assertEqual(artist.read_text(), 'original artist data')
            self.assertFalse((destination / 'starter.svg').exists())

    def test_linked_sources_and_config_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory); root = self.project(parent); external = parent / 'private.svg'; external.write_text('untouched')
            (root / 'raw/art/linked.svg').symlink_to(external)
            with self.assertRaises(ValueError): configure_art_root(root, str(parent / 'art'), seed=True)
            self.assertFalse((parent / 'art').exists())
            (root / 'tools/local.json').symlink_to(external)
            with self.assertRaises(ValueError): configure_art_root(root, str(parent / 'art'), seed=True)
            self.assertEqual(external.read_text(), 'untouched')


if __name__ == '__main__': unittest.main()
