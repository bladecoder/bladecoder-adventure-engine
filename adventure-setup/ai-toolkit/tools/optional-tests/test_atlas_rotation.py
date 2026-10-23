"""Optional actual pinned atlas reader contract."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))

class AtlasRotationTests(unittest.TestCase):
    def test_rotated_region_uses_packed_dimensions_for_page_bounds(self):
        import os
        from atlas_tools import packer_classpath
        from ink_tools import java_program
        from art_pipeline import run
        root=Path(__file__).resolve().parents[2]
        try:jars,_=packer_classpath(root,{})
        except ValueError:self.skipTest('Pinned LibGDX reader unavailable')
        java=java_program({},root)
        if not java:self.skipTest('JDK required')
        with tempfile.TemporaryDirectory() as temp:
            folder=Path(temp);(folder/'page.png').write_bytes(b'page existence is sufficient for metadata reader')
            atlas=folder/'rotated.atlas';atlas.write_text('page.png\nsize: 12, 8\nformat: RGBA8888\nfilter: Linear, Linear\nrepeat: none\narm\n  rotate: true\n  xy: 2, 2\n  size: 4, 10\n  orig: 4, 10\n  offset: 0, 0\n  index: -1\n')
            result=run([java,'-cp',os.pathsep.join(map(str,jars)),root/'ai-toolkit/tools/java/AtlasVerifier.java',atlas])
            region=json.loads(result.split('ATLAS_DATA=',1)[1])['regions'][0]
            self.assertTrue(region['rotate']);self.assertEqual((region['originalWidth'],region['originalHeight']),(4,10))
            atlas.write_text(atlas.read_text().replace('xy: 2, 2','xy: 3, 2'))
            with self.assertRaisesRegex(ValueError,'Invalid region bounds'):run([java,'-cp',os.pathsep.join(map(str,jars)),root/'ai-toolkit/tools/java/AtlasVerifier.java',atlas])
