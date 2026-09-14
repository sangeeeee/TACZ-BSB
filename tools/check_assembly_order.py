"""Audit every generated assembly against the pinned upstream recipe's original operation order.
Run: python tools/check_assembly_order.py path/to/create-tacz.jar
"""
import json
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def original_id(value):
    if value == 'tacz_bsb:precise_ammo': return 'tacz:ammo'
    if isinstance(value, str):
        for prefix in ('high_', 'hardened_', 'precise_'):
            if value.startswith('tacz_bsb:' + prefix): return 'tacz_c:' + value.removeprefix('tacz_bsb:' + prefix)
        return value
    if isinstance(value, list): return [original_id(v) for v in value]
    if isinstance(value, dict): return {k: original_id(v) for k, v in value.items()}
    return value

def operation(step):
    # Item identity during processing intentionally differs; do not discard consumed inputs or machine settings.
    step = original_id(step)
    step['ingredients'] = step['ingredients'][1:]
    for output in step['results']: output['id'] = 'transition'
    return step

with zipfile.ZipFile(sys.argv[1]) as upstream:
    count = 0
    hardened = 0
    for path in sorted((root / 'src/main/resources/data/tacz_bsb/recipe').glob('*.json')):
        current = json.loads(path.read_text(encoding='utf-8'))
        if current['type'] != 'create:sequenced_assembly': continue
        if path.stem.startswith('hardened_'):
            base = path.stem.removeprefix('hardened_').removesuffix('_assembly')
            transition = 'tacz_bsb:unfinished_hardened_' + base
            assert current['loops'] == 1
            assert current['ingredient'] == {'item': 'tacz_c:' + base}
            assert current['transitional_item'] == {'id': transition}
            assert current['results'] == [{'id': 'tacz_bsb:hardened_' + base, 'count': 1}]
            assert [s['type'] for s in current['sequence']] == ['create:filling', 'create:pressing', 'create:filling']
            for step, fluid in zip(current['sequence'], ['minecraft:lava', None, 'minecraft:water']):
                expected = [{'item': transition}]
                if fluid: expected.append({'type': 'neoforge:single', 'amount': 100, 'fluid': fluid})
                assert step['ingredients'] == expected
                assert step['results'] == [{'id': transition}]
            assert not path.with_name('hardened_' + base + '_smelting.json').exists()
            hardened += 1
            continue
        name = path.stem.removeprefix('high_').removeprefix('precise_')
        original = json.loads(upstream.read(f'data/tacz_c/recipe/{name}.json'))
        assert current['loops'] == original['loops'], path.name
        assert original_id(current['ingredient']) == original['ingredient'], path.name
        assert original_id(current['results']) == original['results'], path.name
        assert [operation(s) for s in current['sequence']] == [operation(s) for s in original['sequence']], path.name
        count += 1
    assert count == 47, count
    assert hardened == 3, hardened
    print(f'All {hardened} hardening assemblies: 100 mB lava, one press, 100 mB water, one loop, no smelting.')
    print(f'All {count} assemblies preserve upstream order, consumed quantities, machine settings, loops and output counts.')
