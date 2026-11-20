#!/usr/bin/env python3
"""Збирає values-<locale>/strings.xml з англійського оригіналу та файлу «name|текст».
Для множин: name|one|few|many|other. Перевіряє наявність усіх рядків і збіг плейсхолдерів."""
import re, sys, xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape

res = Path(__file__).resolve().parent.parent / 'app/src/main/res'
src, txt, out = sys.argv[1], Path(sys.argv[2]), sys.argv[3]
root = ET.parse(res / 'values/strings.xml').getroot()
tr = {}
for e in root:
    if e.tag in ('string', 'plurals') and e.get('translatable') != 'false':
        tr[e.get('name')] = e
PH = re.compile(r'%(?:\d\$)?[sd]|%%|\\n|\\u[0-9a-fA-F]{4}')
def ph(s):
    return sorted(PH.findall(s))
new = {}
for ln in txt.read_text(encoding='utf-8').splitlines():
    if not ln.strip():
        continue
    name, *rest = ln.split('|')
    new[name] = rest
bad = []
missing = [n for n in tr if n not in new and n not in ('app_name', 'app_label')]
extra = [n for n in new if n not in tr]
lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
for n, e in tr.items():
    if n not in new:
        continue
    t = new[n]
    if e.tag == 'string':
        if len(t) != 1:
            bad.append(f'{n}: кілька частин'); continue
        en = ''.join(e.itertext()).replace("\\'", "'")
        if ph(en.replace("'", "\\'")) != ph(t[0]) and ph(''.join(e.itertext())) != ph(t[0]):
            bad.append(f'{n}: плейсхолдери {ph("".join(e.itertext()))} != {ph(t[0])}')
        v = escape(t[0])
        if "'" in v or '"' in v:
            bad.append(f'{n}: незаекранована лапка')
        lines.append(f'    <string name="{n}">{v}</string>')
    else:
        if len(t) != 4:
            bad.append(f'{n}: потрібно 4 форми, є {len(t)}'); continue
        en_items = [''.join(i.itertext()) for i in e]
        for q, v in zip(('one', 'few', 'many', 'other'), t):
            if "'" in v or '"' in v:
                bad.append(f'{n}/{q}: лапка')
            if '%' in en_items[-1] and not ph(v):
                bad.append(f'{n}/{q}: немає плейсхолдера')
        lines.append(f'    <plurals name="{n}">')
        for q, v in zip(('one', 'few', 'many', 'other'), t):
            lines.append(f'        <item quantity="{q}">{escape(v)}</item>')
        lines.append('    </plurals>')
lines.append('</resources>')
d = res / out
d.mkdir(parents=True, exist_ok=True)
(d / 'strings.xml').write_text('\n'.join(lines) + '\n', encoding='utf-8')
ET.parse(d / 'strings.xml')  # валідний XML
print(f'записано {len(new)} з {len(tr)}; відсутні: {missing}; зайві: {extra}')
print('\n'.join(bad) or 'плейсхолдери та лапки в порядку')
