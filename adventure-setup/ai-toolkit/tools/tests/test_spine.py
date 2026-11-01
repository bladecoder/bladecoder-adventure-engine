"""Spine cutout adaptation and source/output invariants without a licensed editor."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from spine_tools import validate_data, adapt_data, apply
from asset_manifest import validate_one
from art_pipeline import sha


def skeleton():
    return {'skeleton':{'spine':'4.3.23'},'bones':[{'name':'root'},{'name':'head','parent':'root'}],
            'slots':[{'name':'face','bone':'head','attachment':'open'}],
            'skins':[{'name':'default','attachments':{'face':{'open':{'width':16,'height':16},'closed':{'width':16,'height':16}}}}],
            'animations':{'stand.right':{'slots':{'face':{'attachment':[{'name':'open'},{'time':1,'name':'closed'},{'time':1.1,'name':'open'}]}}}}}


def edits():return {'schema_version':1,'bone_offsets':{},'attachment_paths':{},'animations':{},'replace_animations':[]}


class CutoutTests(unittest.TestCase):
    def test_animation_edit_preserves_template_and_unedited_animation(self):
        source=skeleton();before=copy.deepcopy(source);change=edits();change['animations']={'talk.right':{'bones':{'head':{'rotate':[{}, {'time':.2,'value':5}]}}}}
        changed=adapt_data(source,change)
        self.assertEqual(source,before);self.assertEqual(changed['animations']['stand.right'],before['animations']['stand.right'])
        self.assertIn('talk.right',changed['animations']);self.assertEqual(changed['skins'],before['skins'])
    def test_existing_timeline_replacement_is_explicit(self):
        change=edits();change['animations']={'stand.right':{}}
        with self.assertRaisesRegex(ValueError,'explicit'):adapt_data(skeleton(),change)
        change['replace_animations']=['stand.right'];self.assertEqual(adapt_data(skeleton(),change)['animations']['stand.right'],{})
    def test_invalid_parent_slot_attachment_and_timeline_references_rejected(self):
        mutations=[lambda d:d['bones'][1].update(parent='absent'),lambda d:d['slots'][0].update(bone='missing'),lambda d:d['slots'][0].update(attachment='absent'),lambda d:d['animations']['stand.right']['slots']['face']['attachment'][0].update(name='absent'),lambda d:d['animations']['stand.right'].update(bones={'absent':{}}),lambda d:d['animations']['stand.right'].update(drawOrder=[{'offsets':[{'slot':'absent','offset':1}]}])]
        for mutate in mutations:
            data=skeleton();mutate(data)
            with self.assertRaises(ValueError):validate_data(data)
    def test_missing_images_and_path_escape_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError,'Missing attachment image'):validate_data(skeleton(),temp)
        change=edits();change['attachment_paths']={'default/face/open':'../escape'}
        with self.assertRaises(ValueError):adapt_data(skeleton(),change)
        change['attachment_paths']={'default/face/absent':'valid'}
        with self.assertRaisesRegex(ValueError,'Unknown attachment'):adapt_data(skeleton(),change)
    def test_nonfinite_and_unsorted_keyframes_rejected(self):
        for frames in [[{'time':float('inf')}],[{'time':1},{'time':.2}],[{'time':-1}]]:
            data=skeleton();data['animations']['stand.right']={'bones':{'head':{'rotate':frames}}}
            with self.assertRaises(ValueError):validate_data(data)
        change=edits();change['bone_offsets']={'head':{'rotation':float('nan')}}
        with self.assertRaises(ValueError):adapt_data(skeleton(),change)
    def test_mesh_is_reported_as_unsupported_instead_of_silently_modified(self):
        data=skeleton();data['skins'][0]['attachments']['face']['open']['type']='mesh'
        with self.assertRaisesRegex(ValueError,'region attachments'):validate_data(data)
    def test_spine_piece_manifest_has_dedicated_destination(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);manifest={'schema_version':1,'id':'piece','source':'dummy.svg','exports':[{'id':'face','selection':{'include':['face'],'exclude':[]},'crop':{'mode':'canvas','x':0,'y':0,'width':16,'height':16},'render':{'scale':{'mode':'pixels','width':16,'height':16},'background':{'color':'#000000','opacity':0}},'output':'raw/images/spine-test/face.png','destination':{'kind':'spine-piece','character':'test'}}]}
            validate_one(manifest,root,{},set(),set(),True)
            manifest['exports'][0]['output']='raw/images/office/face.png'
            with self.assertRaisesRegex(ValueError,'Spine pieces'):validate_one(manifest,root,{},set(),set(),True)


class ApplyTests(unittest.TestCase):
    def test_stale_candidate_prevents_every_write(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)/'repo';art=Path(temp)/'art';out=Path(temp)/'preview'
            root.mkdir();art.mkdir();out.mkdir();source=art/'template/input.spine';source.parent.mkdir();source.write_text('original editable')
            target_art=art/'characters/Pilot/Pilot.spine';target_repo=root/'assets/spine/Pilot.skel'
            candidate=out/'Pilot.skel';candidate.write_text('export');editable=out/'Pilot.spine';editable.write_text('derived')
            recipe={'id':'pilot','output':{'editable':'characters/Pilot/Pilot.spine'}}
            report={'schema_version':1,'kind':'spine-preview','recipe':'recipe.json','art_root':str(art),'inputs':{str(source):sha(source)},'files':[{'root':'repo','output':'assets/spine/Pilot.skel','preview':'Pilot.skel','sha256':sha(candidate),'baseline':None},{'root':'art','output':'characters/Pilot/Pilot.spine','preview':'Pilot.spine','sha256':'wrong','baseline':None}]}
            path=out/'report.json';path.write_text(json.dumps(report))
            with patch('spine_tools.read_recipe',return_value=(recipe,None,art,source,source.parent)):
                with self.assertRaisesRegex(ValueError,'changed since preview'):apply(root,{},path)
            self.assertFalse(target_repo.exists());self.assertFalse(target_art.exists());self.assertEqual(source.read_text(),'original editable')


if __name__=='__main__':unittest.main()
