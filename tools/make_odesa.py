#!/usr/bin/env python3
"""Збирає одеську версію з uk.txt: словникові заміни + ручні переписування з odesa_overrides.txt."""
from pathlib import Path
import re
t = Path(__file__).parent
words = [('Что ', 'Шо '), ('что ', 'шо '), ('Нет ', 'Нету '), ('Не удалось', 'Не получилось'), ('Нужно', 'Таки нужно')]
ov = {}
for ln in (t / 'odesa_overrides.txt').read_text(encoding='utf-8').splitlines():
    if '|' in ln:
        k, v = ln.split('|', 1); ov[k] = v
out = []
for ln in (t / 'ru.txt').read_text(encoding='utf-8').splitlines():
    if '|' not in ln:
        continue
    name, rest = ln.split('|', 1)
    if name in ov:
        out.append(f'{name}|{ov[name]}')
    else:
        for a, b in words:
            rest = rest.replace(a, b)
        out.append(f'{name}|{rest}')
(t / 'odesa.txt').write_text('\n'.join(out) + '\n', encoding='utf-8')
print(len(out), 'рядків')
