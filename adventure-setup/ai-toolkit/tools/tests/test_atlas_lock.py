"""Reject unrelated packer binaries even when their filenames match."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from atlas_tools import packer_classpath

class PackerLockTests(unittest.TestCase):
    def test_wrong_binary_rejected_even_if_filename_matches(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);settings=root/'ai-toolkit/tools/asset-settings';settings.mkdir(parents=True)
            (settings/'packer-lock.json').write_text(json.dumps({'group':'test','jars':[{'artifact':'gdx','version':'1','sha256':'wrong'}]}))
            jar=root/'gdx.jar';jar.write_bytes(b'wrong binary')
            with self.assertRaisesRegex(ValueError,'Pinned'):
                packer_classpath(root,{'gdx_jar':str(jar)})
