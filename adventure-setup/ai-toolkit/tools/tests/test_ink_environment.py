"""Compiler discovery follows the generated game's pinned runtime version."""
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ink_tools import compiler_environment


class InkEnvironment(unittest.TestCase):
    def test_only_matching_version_is_selected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root / 'gradle.properties').write_text('bladeInkVersion=1.3.2\n')
            for artifact in ('blade-ink', 'blade-ink-compiler'):
                for version in ('1.3.2', '9.9.9'):
                    target = root / 'cache/com.bladecoder.ink' / artifact / version / 'hash' / (artifact + '-' + version + '.jar')
                    target.parent.mkdir(parents=True); target.write_bytes(b'fixture-discovery-only')
            with patch.dict(os.environ, {'GRADLE_USER_HOME': '/unavailable-default-cache'}):
                result = compiler_environment(root, {'gradle_cache': 'cache'})
            self.assertEqual(result['version'], '1.3.2')
            self.assertEqual([p.name for p in result['runtime']], ['blade-ink-1.3.2.jar'])
            self.assertEqual([p.name for p in result['compilers']], ['blade-ink-compiler-1.3.2.jar'])


if __name__ == '__main__': unittest.main()
