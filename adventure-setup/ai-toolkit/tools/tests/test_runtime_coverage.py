"""Coverage uses executed source identities; loaded definitions alone do not count."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_scenario import coverage


class CoverageTests(unittest.TestCase):
    def test_scene_qualified_sources_actions_and_unknowns_are_distinct(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            base = {'resource': 'model/00.chapter.json', 'actor': 'Joe', 'verb': 'lookat', 'action_count': 2}
            bank = dict(base, scene='bank', json_pointer='/scenes/bank/actors/Joe/verbs/lookat', source='model/00.chapter.json#/scenes/bank/actors/Joe/verbs/lookat')
            office = dict(base, scene='office', json_pointer='/scenes/office/actors/Joe/verbs/lookat', source='model/00.chapter.json#/scenes/office/actors/Joe/verbs/lookat')
            ref = dict(bank, action_pointer=bank['json_pointer'] + '/actions/0')
            events = [{'type': 'action.started', 'data': {'source_ref': ref}}, {'type': 'action.completed', 'data': {'source_ref': ref}},
                      {'type': 'action.started', 'frame': 10, 'data': {'runner_id': 'unmapped', 'action': 'Unknown'}}]
            (out / 'events.jsonl').write_text('\n'.join(json.dumps(e) for e in events))
            result = coverage(out, {'source_catalogue': [bank, office]})
            self.assertTrue(result['source_coverage_available'])
            self.assertEqual(result['observed_sources'], [bank['source']])
            self.assertEqual(result['unobserved_actor_verbs'], [office])
            self.assertEqual(len(result['unobserved_model_actions']), 3)
            self.assertEqual(result['started_model_actions'], result['completed_model_actions'])
            self.assertEqual(result['unmapped_action_events'][0]['runner_id'], 'unmapped')

    def test_external_false_result_and_choice_revision_are_retained(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            events = [{'type': 'ink.external.call', 'data': {'call_id': 1, 'name': 'inInventory', 'arguments': ['gum'], 'path': 'bank.Joe.4'}},
                      {'type': 'ink.external.result', 'data': {'call_id': 1, 'name': 'inInventory', 'result': False}},
                      {'type': 'dialogue.choices', 'data': {'choice_set': 7, 'choices': ['Leave', 'Ask']}},
                      {'type': 'command.started', 'data': {'operation': 'dialogue.choose', 'arguments': {'choice_set': 7, 'option': 1}}},
                      {'type': 'command.started', 'command_id': 'fixture', 'data': {'operation': 'fixture.scene'}},
                      {'type': 'scene.changed', 'command_id': 'fixture', 'data': {'scene': 'bank'}}]
            (out / 'events.jsonl').write_text('\n'.join(json.dumps(e) for e in events))
            result = coverage(out, {})
            self.assertFalse(result['source_coverage_available'])
            self.assertIs(result['external_calls'][0]['terminal']['result'], False)
            self.assertEqual(result['selected_choices'][0]['text'], 'Ask')
            self.assertTrue(result['scene_transitions'][0]['setup'])
            self.assertTrue((out / 'transcript.json').is_file())


if __name__ == '__main__': unittest.main()
