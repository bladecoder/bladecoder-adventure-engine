"""Exploration policy cannot invent hidden actions or mistake ambient time for progress."""
from copy import deepcopy
from pathlib import Path
import random
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_explore import available_actions, canonical, progress_digest, select_action


class ExplorationTests(unittest.TestCase):
    def player(self):
        return {'assetState': 'LOADED', 'paused': False, 'cutMode': False,
                'actors': [{'id': 'Joe', 'verbs': ['talkto', 'lookat']}, {'id': 'Door', 'verbs': ['lookat']}],
                'inventory': [{'id': 'gum', 'verbs': ['use', 'lookat']}], 'dialogOptions': []}

    def test_only_projected_actors_and_acquired_inventory_are_candidates(self):
        actions, skipped = available_actions(self.player())
        self.assertEqual(skipped, [])
        uses = [v for v in actions if v.get('verb') == 'use']
        self.assertEqual(uses, [{'kind': 'verb', 'actor': 'gum', 'verb': 'use', 'target': 'Door'},
                              {'kind': 'verb', 'actor': 'gum', 'verb': 'use', 'target': 'Joe'}])
        self.assertFalse(any(v.get('target') == v.get('actor') for v in actions))
        self.assertFalse(any(v.get('actor') == 'Hidden' for v in actions))

    def test_choice_mode_excludes_actor_actions_and_duplicate_text(self):
        player = self.player(); player.update(cutMode=True, dialogOptions=['Sí', 'No', 'Sí'])
        actions, skipped = available_actions(player)
        self.assertEqual(actions, [{'kind': 'choose', 'text': 'No'}])
        self.assertEqual(skipped, [{'reason': 'ambiguous_choice_text', 'text': 'Sí'}])
        player['paused'] = True
        self.assertEqual(available_actions(player)[0], [])

    def test_seeded_order_is_stable_and_prefers_untried_state_actions(self):
        actions, _ = available_actions(self.player()); seen = set()
        first = select_action(actions, 'state', seen, random.Random(1701))
        self.assertEqual(first, select_action(actions, 'state', seen, random.Random(1701)))
        seen.add(('state', canonical(first)))
        self.assertNotEqual(first, select_action(actions, 'state', seen, random.Random(1701)))
        self.assertIsNone(select_action([], 'state', seen, random.Random(1)))

    def test_progress_excludes_ambient_motion_but_tracks_puzzle_state(self):
        state = {'chapter': '00', 'scene': {'id': 'bank', 'actors': [{'id': 'Joe', 'visible': True, 'state': None, 'x': 2}]},
                 'inventory': [], 'properties': {}, 'ink': {'variables': {'attempts': {'type': 'Integer', 'value': 0}}}, 'frame': 1}
        changed = deepcopy(state); changed['frame'] = 200; changed['scene']['actors'][0]['x'] = 900
        self.assertEqual(progress_digest(state), progress_digest(changed))
        changed['ink']['variables']['attempts']['value'] = 1
        self.assertNotEqual(progress_digest(state), progress_digest(changed))


if __name__ == '__main__':
    unittest.main()
