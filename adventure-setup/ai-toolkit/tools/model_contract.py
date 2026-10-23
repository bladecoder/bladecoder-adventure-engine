"""Merge explicit project action metadata without changing built-in engine contracts."""
import json
from pathlib import Path
from project_config import load_project


def extend_contract(root, contract):
    root = Path(root).resolve()
    if not (root / 'adventure.json').exists():
        return
    selected = load_project(root).get('model_contract')
    if not selected:
        return
    path = (root / selected).resolve()
    if not path.is_relative_to(root):
        raise ValueError('Project action contract escapes the repository.')
    extension = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(extension, dict) or type(extension.get('schema_version')) is not int or extension['schema_version'] != 1 or not isinstance(extension.get('actions'), dict):
        raise ValueError('Project action contract requires schema_version 1 and an actions object.')
    if extension.get('engine_version', contract['engine_version']) != contract['engine_version']:
        raise ValueError('Project action contract targets a different engine version.')
    additions = {}
    for alias, info in extension['actions'].items():
        if alias in contract['actions']:
            raise ValueError('Project action contract cannot override a built-in action: ' + alias)
        if not isinstance(info, dict) or set(info) != {'class', 'fields'} or not isinstance(info.get('class'), str) or '.' not in info['class'] or not isinstance(info.get('fields'), dict):
            raise ValueError('Project action must declare a qualified class and fields object.')
        for name, field in info['fields'].items():
            if not isinstance(field, dict) or not {'java_type', 'semantic_type', 'required_hint', 'initializer'} <= set(field):
                raise ValueError('Project action field metadata is incomplete: ' + name)
            if not isinstance(field['java_type'], str) or not isinstance(field['semantic_type'], str) or type(field['required_hint']) is not bool:
                raise ValueError('Project action field metadata has invalid types: ' + name)
            if 'enum' in field and (not isinstance(field['enum'], list) or not all(isinstance(item, str) for item in field['enum'])):
                raise ValueError('Project action enum must list strings.')
        additions[alias] = info
    contract['actions'].update(additions)
