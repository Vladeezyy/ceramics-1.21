"""Exact-string edits for porting: every old string must be found, otherwise the script stops."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / 'src/main/java/knightminer/ceramics'


def sub(rel, pairs, imports_remove=(), imports_add=()):
    path = ROOT / rel
    text = path.read_text(encoding='utf-8')
    for old, new in pairs:
        if old in text:
            text = text.replace(old, new)
            continue
        # fall back to matching with any whitespace (indentation, blank lines)
        pattern = r'\s+'.join(re.escape(part) for part in old.split())
        text, n = re.subn(pattern, lambda m: new, text)
        if n == 0:
            sys.exit(f'{rel}: not found: {old[:80]!r}')
    for imp in imports_remove:
        line = f'import {imp};\n'
        if line not in text:
            sys.exit(f'{rel}: import not found: {imp}')
        text = text.replace(line, '')
    if imports_add:
        lines = ''.join(f'import {imp};\n' for imp in imports_add if f'import {imp};\n' not in text)
        m = re.search(r'^import ', text, re.M)
        pos = m.start() if m else text.index('\n', text.index('package ')) + 2
        text = text[:pos] + lines + text[pos:]
    path.write_text(text, encoding='utf-8')
    print('edited', rel)


def regex(rel, pairs):
    path = ROOT / rel
    text = path.read_text(encoding='utf-8')
    for pattern, new in pairs:
        text, n = re.subn(pattern, new, text, flags=re.M)
        if n == 0:
            sys.exit(f'{rel}: regex not found: {pattern!r}')
    path.write_text(text, encoding='utf-8')
