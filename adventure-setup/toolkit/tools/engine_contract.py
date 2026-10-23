#!/usr/bin/env python3
"""Extract reviewable action metadata from a specified engine source checkout.

No Java execution or network; output goes only to the explicitly requested file.
This extractor targets the pinned Java source style and fails on unmatched fields.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def extract(engine, project=None):
    files = sorted((engine / 'blade-engine/src').rglob('*.java'))
    files += sorted((engine / 'blade-engine-spine-plugin/src').rglob('*.java'))
    if project is not None: files += sorted((project / 'core/src/main/java').rglob('*.java'))
    classes, hashes = {}, {}
    enums = {}
    for path in files:
        text = path.read_text()
        # Strip comments, preserving annotations and literals in this source style.
        text = re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)
        package = re.search(r'package\s+([\w.]+);', text)
        cls = re.search(r'\bclass\s+(\w+)(?:\s+extends\s+(\w+))?', text)
        if not package or not cls:
            continue
        name, parent = cls.groups()
        qualified = package[1] + '.' + name
        local_enums = {}
        for match in re.finditer(r'\benum\s+(\w+)\s*\{([^}]+)\}', text, re.S):
            options = match[2].split(';')[0]
            options = re.sub(r'\([^()]*\)', '', options)
            values = re.findall(r'\b([A-Z][A-Z_0-9]*)\b\s*(?:,|$)', options.strip())
            if values:
                enums[name + '.' + match[1]] = values
                local_enums[match[1]] = values
        fields = {}
        pattern = r'@ActionProperty\b(?:\(([^)]*)\))?\s*(?:@\w+(?:\((?:[^()\"]|\"(?:\\.|[^\"\\])*\")*\))?\s*)*(?:(?:private|protected|public|final)\s+)*(\w+(?:\.\w+)?)\s+(\w+)\s*(?:=\s*([^;]*))?;'
        for match in re.finditer(pattern, text, re.S):
            annotation, java_type, field, initial = match.groups()
            annotation = annotation or ''
            semantic = re.search(r'type\s*=\s*(?:Param\.)?Type\.(\w+)', annotation)
            default = re.search(r'defaultValue\s*=\s*"([^"]*)"', annotation)
            fields[field] = {'java_type': java_type,
                             'required_hint': bool(re.search(r'required\s*=\s*true', annotation)),
                             'semantic_type': semantic[1] if semantic else 'NOT_SET',
                             'default_hint': default[1] if default else None,
                             'initializer': initial.strip() if initial else None}
            if java_type in local_enums:
                fields[field]['enum'] = local_enums[java_type]
        if len(fields) != len(re.findall(r'@ActionProperty\b', text)):
            raise ValueError(f'Unparsed annotated field in {path}')
        description = re.search(r'@ActionDescription\((.*?)\)\s*(?:public|abstract|final|\s)*class', text, re.S)
        tag = None
        if description:
            override = re.search(r'name\s*=\s*"([^"]+)"', description[1])
            tag = override[1] if override else re.sub(r'Action$', '', name)
        classes[name] = {'class': qualified, 'parent': parent, 'fields': fields, 'tag': tag}
        if fields or tag or name in ('ActionFactory', 'ActionUtils', 'Param', 'SceneActorRef', 'ActorAnimationRef'):
            owner = 'project' if project is not None and path.is_relative_to(project) else 'engine'
            relative = path.relative_to(project if owner == 'project' else engine).as_posix()
            hashes[owner + '/' + relative] = hashlib.sha256(path.read_bytes()).hexdigest()
    factory = (engine / 'blade-engine/src/com/bladecoder/engine/actions/ActionFactory.java').read_text()
    registered = set(re.findall(r'addClassTag\((\w+)\.class\)', factory))

    def inherited(name):
        info = classes[name]
        fields = inherited(info['parent']) if info['parent'] in classes else {}
        fields.update(info['fields'])
        return fields

    actions = {}
    for name, info in sorted(classes.items()):
        if not info['tag']:
            continue
        fields = inherited(name)
        for field in fields.values():
            if field['java_type'] in enums:
                field['enum'] = enums[field['java_type']]
            elif field['java_type'] == 'InterpolationMode':
                # Enum has methods after its constants; extract the list directly.
                src = (engine / 'blade-engine/src/com/bladecoder/engine/util/InterpolationMode.java').read_text()
                body = src.split('enum InterpolationMode {', 1)[1].split(';', 1)[0]
                field['enum'] = re.findall(r'\b([A-Z][A-Z_0-9]*)\b', body)
        entry = {'class': info['class'], 'fields': fields}
        actions[info['class']] = entry
        if name in registered:
            actions[info['tag']] = entry
    revision = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=engine, capture_output=True, text=True, check=True).stdout.strip()
    version = re.search(r'^version=(.*)$', (engine/'gradle.properties').read_text(), re.M)[1]
    if version not in ('4.4.0', '4.5.0-SNAPSHOT'):
        raise ValueError('Unreviewed engine version; review MODEL serializers before updating supported versions.')
    for path in files:
        if path.is_relative_to(engine) and any(part in path.parts for part in ('model','serialization','anim','ink','spine')):
            hashes['engine/'+path.relative_to(engine).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    hashes['engine/gradle.properties'] = hashlib.sha256((engine/'gradle.properties').read_bytes()).hexdigest()
    return {'schema_version': 1, 'engine_version': version, 'engine_revision': revision,
            'source_sha256': hashes, 'actions': actions,
            'notes': 'required_hint is editor metadata, not a runtime presence rule; primitive initializers remain valid when omitted.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--engine', required=True, type=Path)
    parser.add_argument('--project', type=Path, help='Optional project custom-action sources.')
    parser.add_argument('--custom-only', action='store_true', help='Emit only project actions for a project extension contract.')
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    result = extract(args.engine.resolve(), args.project.resolve() if args.project else None)
    if args.custom_only:
        if not args.project: parser.error('--custom-only requires --project')
        result['actions'] = {key: value for key, value in result['actions'].items() if not value['class'].startswith('com.bladecoder.engine.')}
        result['source_sha256'] = {key: value for key, value in result['source_sha256'].items() if key.startswith('project/')}
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    print(f"Extracted {len(result['actions'])} action aliases/classes.")


if __name__ == '__main__':
    main()
