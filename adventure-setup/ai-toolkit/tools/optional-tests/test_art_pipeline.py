"""Renderer integration and replacement safety. Synthetic sources, real installed tools."""
import copy
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from art_pipeline import export_preview, apply_preview, run, sha, image_info
from atlas_tools import packer_classpath


@unittest.skipUnless(shutil.which('inkscape') and shutil.which('magick'), 'Inkscape and ImageMagick required')
class RenderTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='adventure-render-test-')
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / 'repo with spaces'; self.root.mkdir()
        self.art = self.base / 'art with spaces'; self.art.mkdir()
        self.svg = self.art / 'scene.svg'
        self.svg.write_text('''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="32" height="32" viewBox="0 0 32 32">
<defs><linearGradient id="red"><stop stop-color="red"/></linearGradient><clipPath id="clip"><rect width="16" height="8"/></clipPath></defs>
<rect id="prototype" width="8" height="8" fill="url(#red)"/>
<g style="display:none" transform="translate(8,8)"><g id="chosen" style="display:none" clip-path="url(#clip)"><use xlink:href="#prototype"/><rect x="8" width="8" height="8" fill="blue"/><rect id="excluded" width="32" height="32" fill="green"/></g></g>
<rect id="guide" width="32" height="32" fill="yellow"/>
</svg>''')
        self.manifest = {'schema_version': 1, 'id': 'test', 'source': 'scene.svg', 'exports': [
            {'id': 'object', 'selection': {'include': ['chosen'], 'exclude': ['excluded']},
             'crop': {'mode': 'page'}, 'render': {'scale': {'mode': 'dpi', 'value': 96},
             'background': {'color': '#000000', 'opacity': 0}}, 'output': 'raw/images/test/object.png',
             'destination': {'kind': 'atlas', 'group': 'test'}}]}
        self.recipe = self.root / 'recipe.json'
        self.config = {'art_root': str(self.art)}

    def preview(self, name='preview'):
        self.recipe.write_text(json.dumps(self.manifest))
        out = self.base / name
        checks = export_preview(self.root, self.config, [str(self.recipe)], out)
        self.assertEqual(checks[0]['status'], 'ok', checks)
        return out, json.loads((out/'report.json').read_text())

    def pixel(self, path, x, y):
        return run(['magick', path, '-format', f'%[pixel:p{{{x},{y}}}]', 'info:'])

    def test_hidden_ancestors_transforms_clone_defs_clip_and_exclusion(self):
        before = sha(self.svg)
        out, report = self.preview()
        png = out / report['files'][0]['preview']
        self.assertIn('255,0,0,1', self.pixel(png, 9, 9))
        self.assertIn('0,0,255,1', self.pixel(png, 17, 9))
        self.assertTrue(self.pixel(png, 0, 0).endswith(',0)'))
        self.assertTrue(self.pixel(png, 9, 17).endswith(',0)'))
        self.assertEqual(sha(self.svg), before)
        self.assertFalse((self.root/'raw').exists())
        self.assertEqual(apply_preview(self.root, out/'report.json')[0]['status'], 'ok')
        self.assertTrue((self.root/'raw/images/test/object.png').exists())

    def test_stale_source_and_candidate_prevent_all_replacements(self):
        out, report = self.preview()
        self.svg.write_text(self.svg.read_text() + '\n')
        self.assertEqual(apply_preview(self.root, out/'report.json')[0]['status'], 'error')
        self.assertFalse((self.root/'raw').exists())
        out, report = self.preview('second')
        (out/report['files'][0]['preview']).write_bytes(b'changed')
        self.assertEqual(apply_preview(self.root, out/'report.json')[0]['status'], 'error')
        self.assertFalse((self.root/'raw').exists())

    def test_linked_image_with_percent_encoded_name_and_xml_base(self):
        sub = self.art/'images';sub.mkdir()
        run(['magick','-size','4x4','xc:cyan',sub/'a b.png'])
        self.svg.write_text('''<svg xmlns="http://www.w3.org/2000/svg" width="4" height="4" xml:base="images/"><image id="chosen" width="4" height="4" href="a%20b.png"/></svg>''')
        self.manifest['exports'][0]['selection']['exclude'] = []
        out, report = self.preview()
        self.assertTrue(report['files'][0]['image']['opaque'])
        self.assertIn('0,255,255', self.pixel(out/report['files'][0]['preview'], 1, 1))
        self.assertIn(str((sub/'a b.png').resolve()), report['inputs'])

    def test_canvas_and_nine_patch_markers(self):
        export = self.manifest['exports'][0]
        export['crop'] = {'mode': 'canvas','x':8,'y':8,'width':16,'height':8}
        export['render']['scale'] = {'mode':'pixels','width':16,'height':8}
        export['output'] = 'raw/images/test/object.9.png'
        export['nine_patch'] = {'mode':'add-border','split':[2,3,1,2],'padding':[1,2,2,1]}
        out, report = self.preview()
        png = out/report['files'][0]['preview']
        self.assertEqual((report['files'][0]['image']['width'],report['files'][0]['image']['height']), (18,10))
        self.assertIn('0,0,0,1', self.pixel(png,3,0))
        self.assertTrue(self.pixel(png,1,0).endswith(',0)'))
        self.assertIn('0,0,0,1', self.pixel(png,0,2))

    def test_shared_source_change_between_manifests_is_rejected(self):
        from art_pipeline import render_one
        self.recipe.write_text(json.dumps(self.manifest))
        second=copy.deepcopy(self.manifest);second['id']='second'
        second['exports'][0]['output']='raw/images/test/second.png'
        other=self.root/'second.json';other.write_text(json.dumps(second))
        calls=0
        def change_after_first(*args,**kwargs):
            nonlocal calls
            result=render_one(*args,**kwargs);calls+=1
            if calls==1:self.svg.write_text(self.svg.read_text()+'\n')
            return result
        with patch('art_pipeline.render_one',side_effect=change_after_first):
            result=export_preview(self.root,self.config,[str(self.recipe),str(other)],self.base/'race')
        self.assertEqual(result[0]['status'],'error')
        self.assertIn('input changed',result[0]['message'])
        self.assertFalse((self.base/'race/report.json').exists())

    def test_requested_aspect_mismatch_is_an_error(self):
        self.manifest['exports'][0]['render']['scale']={'mode':'pixels','width':20,'height':10}
        self.recipe.write_text(json.dumps(self.manifest))
        result=export_preview(self.root,self.config,[str(self.recipe)],self.base/'bad')
        self.assertEqual(result[0]['status'],'error')
        self.assertFalse((self.root/'raw').exists())

    def test_path_escape_and_failed_batch_roll_back(self):
        out, report = self.preview()
        report['files'][0]['output'] = '../outside.png'
        (out/'report.json').write_text(json.dumps(report))
        self.assertEqual(apply_preview(self.root,out/'report.json')[0]['status'],'error')
        self.assertFalse((self.base/'outside.png').exists())
        # Fail the second replacement after the first was written.
        out, report = self.preview('rollback')
        row=copy.deepcopy(report['files'][0]);row['output']='raw/images/test/second.png';report['files'].append(row)
        (out/'report.json').write_text(json.dumps(report))
        original=shutil.copyfile
        def fail_second(src,dest,*args,**kwargs):
            if '.second.png.' in Path(dest).name and str(dest).endswith('.tmp'):raise OSError('injected write failure')
            return original(src,dest,*args,**kwargs)
        with patch('art_pipeline.shutil.copyfile',side_effect=fail_second):
            self.assertEqual(apply_preview(self.root,out/'report.json')[0]['status'],'error')
        self.assertFalse((self.root/'raw/images/test/object.png').exists())


class PackerLockTests(unittest.TestCase):
    def test_wrong_binary_rejected_even_if_filename_matches(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);settings=root/'ai-toolkit/tools/asset-settings';settings.mkdir(parents=True)
            (settings/'packer-lock.json').write_text(json.dumps({'group':'test','jars':[{'artifact':'gdx','version':'1','sha256':'wrong'}]}))
            jar=root/'gdx.jar';jar.write_bytes(b'wrong binary')
            with self.assertRaisesRegex(ValueError,'Pinned'):
                packer_classpath(root,{'gdx_jar':str(jar)})


class PackerIntegrationTests(unittest.TestCase):
    def test_real_packer_preserves_indices_original_canvas_and_nine_patch(self):
        from atlas_tools import pack_preview
        tools=Path(__file__).resolve().parents[1]
        try:
            packer_classpath(tools.parent,{})
        except ValueError:
            self.skipTest('Pinned packer is not cached')
        if not shutil.which('java') or not shutil.which('magick'):
            self.skipTest('JDK and ImageMagick required')
        with tempfile.TemporaryDirectory(prefix='adventure-pack-test-') as temporary:
            root=Path(temporary)/'repo';root.mkdir()
            shutil.copytree(tools/'asset-settings',root/'ai-toolkit/tools/asset-settings')
            (root/'ai-toolkit/tools/java').mkdir(parents=True)
            shutil.copyfile(tools/'java/AtlasVerifier.java',root/'ai-toolkit/tools/java/AtlasVerifier.java')
            source=root/'raw/images/test';source.mkdir(parents=True)
            for index,color in [(0,'red'),(1,'blue')]:
                run(['magick','-size','20x20','xc:none','-fill',color,'-draw','rectangle 5,5 14,14',source/f'pulse_{index}.png'])
            nine=source/'border.9.png'
            run(['magick','-size','20x20','xc:white',nine])
            from art_pipeline import add_nine_patch
            add_nine_patch('magick',nine,{'split':[2,3,4,5],'padding':[3,4,5,6]},20,20)
            out=Path(temporary)/'review'
            result=pack_preview(root,{},'test',out)
            self.assertEqual(result[0]['status'],'ok',result)
            report=json.loads((out/'report.json').read_text())
            regions=report['atlas']['regions']
            frames=sorted([r for r in regions if r['name']=='pulse'],key=lambda r:r['index'])
            self.assertEqual([r['index'] for r in frames],[0,1])
            self.assertTrue(all((r['originalWidth'],r['originalHeight'])==(20,20) for r in frames))
            self.assertTrue(all((r['offsetX'],r['offsetY'])==(5,5) for r in frames))
            border=next(r for r in regions if r['name']=='border')
            self.assertEqual(border['split'],[2,3,4,5])
            self.assertEqual(border['pad'],[3,4,5,6])
            (source/'extra.png').write_bytes((source/'pulse_0.png').read_bytes())
            self.assertIn('directory members changed',apply_preview(root,out/'report.json')[0]['message'])
            self.assertFalse((root/'assets').exists())
            (source/'extra.png').unlink()
            self.assertEqual(apply_preview(root,out/'report.json')[0]['status'],'ok')
            self.assertTrue((root/'assets/atlases/1/test.atlas').is_file())
