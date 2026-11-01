import argparse
import unittest
from unittest.mock import patch, Mock
import edit_tools


class EditClientTests(unittest.TestCase):
    def args(self, **kwargs):
        values = dict(action='command', session='/tmp/session', out=None, chapter='00',
                      arguments='{}', spec=None, operation='entity.update', revision=4,
                      request_id='stable-request')
        values.update(kwargs)
        return argparse.Namespace(**values)

    def test_player_session_cannot_edit(self):
        client = Mock(mode='player')
        with patch.object(edit_tools.RuntimeClient, 'connect', return_value=client):
            checks, _ = edit_tools.execute('/tmp', {}, self.args())
        self.assertEqual(checks[0]['status'], 'error')
        client.request.assert_not_called()

    def test_revision_and_request_identity_are_preserved(self):
        client = Mock(mode='edit')
        client.request.side_effect = [{'model': {'revision': 9}}, {'status': 'completed'}]
        with patch.object(edit_tools.RuntimeClient, 'connect', return_value=client):
            checks, result = edit_tools.execute('/tmp', {}, self.args())
        self.assertEqual(checks[0]['status'], 'ok')
        payload = client.request.call_args.args[1]
        self.assertEqual(payload['expected_revision'], 4)
        self.assertEqual(payload['request_id'], 'stable-request')
        self.assertEqual(result['status'], 'completed')

    def test_save_is_explicit_operation(self):
        client = Mock(mode='edit')
        client.request.side_effect = [{'model': {'revision': 9}}, {'status': 'completed'}]
        with patch.object(edit_tools.RuntimeClient, 'connect', return_value=client):
            edit_tools.execute('/tmp', {}, self.args(action='save', revision=None))
        payload = client.request.call_args.args[1]
        self.assertEqual(payload['operation'], 'model.save')
        self.assertEqual(payload['expected_revision'], 9)
