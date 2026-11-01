"""Read the documented LibGDX minimal-JSON subset, without executing game code."""
import bisect
import json
import math
import re


class ModelSyntaxError(ValueError):
    pass


def pointer(base, key):
    return base + '/' + str(key).replace('~', '~0').replace('/', '~1')


class ModelJSON:
    """Quoted strings, bare keys/values, comments, trailing commas; duplicate-safe.

    This is a bounded subset, not a replacement for LibGDX JsonReader. Explicit
    commas are required between members. See ai-toolkit/docs/chapter-format.md for limits.
    """
    def __init__(self, text):
        self.text, self.pos, self.lines = text, 0, {}
        self.newlines = [m.start() for m in re.finditer('\n', text)]

    def fail(self, message):
        line = bisect.bisect_left(self.newlines, self.pos) + 1
        raise ModelSyntaxError(f'{message} at line {line}, offset {self.pos}')

    def skip(self):
        while self.pos < len(self.text):
            if self.text[self.pos].isspace():
                self.pos += 1
            elif self.text.startswith('//', self.pos):
                end = self.text.find('\n', self.pos)
                self.pos = len(self.text) if end < 0 else end
            elif self.text.startswith('/*', self.pos):
                end = self.text.find('*/', self.pos + 2)
                if end < 0:
                    self.fail('Unclosed comment')
                self.pos = end + 2
            else:
                break

    def atom(self, key=False):
        self.skip()
        if self.pos >= len(self.text):
            self.fail('Unexpected end of input')
        if self.text[self.pos] == '"':
            try:
                value, self.pos = json.decoder.scanstring(self.text, self.pos + 1)
            except ValueError as exc:
                self.fail(str(exc))
            return value
        start = self.pos
        delimiters = ':{}[],\r\n' if key else ',]}\r\n'
        while self.pos < len(self.text) and self.text[self.pos] not in delimiters:
            if self.text.startswith(('//', '/*'), self.pos):
                break
            self.pos += 1
        value = self.text[start:self.pos].strip()
        if not value:
            self.fail('Empty token')
        if key:
            return value
        if value in ('NaN', 'Infinity', '-Infinity'):
            self.fail('Non-finite numeric token')
        try:
            result = json.loads(value)
        except ValueError:
            return value
        if isinstance(result, float) and not math.isfinite(result):
            self.fail('Non-finite number')
        return result

    def value(self, path='', depth=0):
        if depth > 160:
            self.fail('Model nesting exceeds supported depth 160')
        self.skip()
        self.lines[path] = bisect.bisect_left(self.newlines, self.pos) + 1
        if self.pos >= len(self.text):
            self.fail('Unexpected end of input')
        char = self.text[self.pos]
        if char not in '{[':
            return self.atom()
        self.pos += 1
        result = {} if char == '{' else []
        end = '}' if char == '{' else ']'
        while True:
            self.skip()
            if self.pos >= len(self.text):
                self.fail('Unclosed container')
            if self.text[self.pos] == end:
                self.pos += 1
                return result
            if char == '{':
                key = self.atom(key=True)
                self.skip()
                if self.pos >= len(self.text) or self.text[self.pos] != ':':
                    self.fail('Missing colon')
                self.pos += 1
                if key in result:
                    self.fail(f'Duplicate member {pointer(path, key)}')
                result[key] = self.value(pointer(path, key), depth + 1)
            else:
                result.append(self.value(pointer(path, len(result)), depth + 1))
            self.skip()
            if self.pos < len(self.text) and self.text[self.pos] == ',':
                self.pos += 1
            elif self.pos >= len(self.text) or self.text[self.pos] != end:
                self.fail('Missing comma')

    def parse(self):
        value = self.value()
        self.skip()
        if self.pos != len(self.text):
            self.fail('Trailing input')
        return value


def load_model(path):
    parser = ModelJSON(path.read_text(encoding='utf-8-sig'))
    return parser.parse(), parser.lines
