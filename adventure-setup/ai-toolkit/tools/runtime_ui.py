"""Actual input routing helpers. Selectors choose coordinates; they never invoke UI listeners."""
import time
from runtime_client import RuntimeFailure, save_json

class InputDriver:

    def __init__(self, driver):
        self.d = driver
        self.client = driver.client
        self.history = []

    def event(self, **arguments):
        job = self.client.command('input.event', arguments)
        self.history.append({'event': arguments, 'command_id': job['command_id']})
        save_json(self.d.out / 'input-history.json', self.history)
        return job['result']

    def frames(self, count=4):
        self.client.command('clock.step', {'frames': count, 'delta': 0.05})

    def ui(self):
        return self.client.state('/ui')['value']

    def widget(self, name=None, text=None):
        matches = [v for v in self.ui()['widgets'] if v['visible'] and v['hit_center'] and (not v['disabled']) and (v['name'] == name if name else v['type'].endswith('TextButton') and text in (v['text'] or ''))]
        if len(matches) != 1:
            raise RuntimeFailure(f'UI selector must resolve to one visible hit-testable widget: name={name!r}, text={text!r}; found {len(matches)}')
        return matches[0]

    def center(self, bounds):
        ui = self.ui()
        point = (int(bounds['x'] + bounds['width'] / 2), int(bounds['y'] + bounds['height'] / 2))
        if not (0 <= point[0] < ui['width'] and 0 <= point[1] < ui['height']):
            raise RuntimeFailure('UI target center is outside the current window.')
        return point

    def click(self, point=None, *, name=None, text=None, actor=None, button=0):
        if name or text:
            target = self.widget(name, text)
            point = self.center(target['bounds'])
        elif actor:
            matches = [a for a in self.ui()['scene_actors'] if a['id'] == actor and a['visible']]
            if len(matches) != 1:
                raise RuntimeFailure('No unique visible actor: ' + actor)
            point = self.center(matches[0]['bounds'])
        if point is None:
            raise RuntimeFailure('A click needs a point or selector.')
        x, y = point
        self.event(type='mouseMoved', x=x, y=y)
        self.frames(1)
        if actor:
            self.d.check(self.ui().get('scene_hit') == actor, 'input hit-test ' + actor)
        self.event(type='touchDown', x=x, y=y, button=button)
        self.event(type='touchUp', x=x, y=y, button=button)
        self.frames(4)
        return self.d.state()

    def key(self, code):
        self.event(type='keyDown', keycode=code)
        self.event(type='keyUp', keycode=code)
        self.frames(2)

    def until(self, predicate, label, advance_text=False):
        deadline = time.monotonic() + 35
        while time.monotonic() < deadline:
            state = self.d.state()
            if predicate(state):
                return state
            self.frames(8)
            if advance_text and state.get('text'):
                self.click((state['ui']['width'] // 2, state['ui']['height'] // 2))
        raise RuntimeFailure('UI readiness watchdog: ' + label)
