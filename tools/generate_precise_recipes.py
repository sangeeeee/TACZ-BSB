"""Generate the checked-in precise-ammunition recipes/models from the pinned Create TaCZ 1.0.2 jar.
Usage: python tools/generate_precise_recipes.py path/to/create-tacz.jar
No runtime dependency on this script or on the original jar's filesystem location.
"""
import copy
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
JAVA = ROOT / 'src/main/java/com/sange/tacz_bsb'
z = zipfile.ZipFile(sys.argv[1])
source = {Path(n).stem: json.loads(z.read(n)) for n in z.namelist()
          if n.startswith('data/tacz_c/recipe/') and n.endswith('.json')}
base_en = json.loads(z.read('assets/tacz_c/lang/en_us.json'))
langs = {code: json.loads((RES / f'assets/tacz_bsb/lang/{code}.json').read_text(encoding='utf-8-sig'))
         for code in ('en_us', 'zh_cn')}
materials = {}
recipes = {}

def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def material(name, base, zh, en=None):
    assert name not in materials
    materials[name] = base
    namespace, path = base.split(':')
    assert namespace == 'minecraft' or f'assets/{namespace}/models/item/{path}.json' in z.namelist(), base
    save(RES / f'assets/tacz_bsb/models/item/{name}.json', {'parent': f'{namespace}:item/{path}'})
    if en is None:
        original = re.sub(r'§.', '', base_en[f'item.{namespace}.{path}'])
        en = ('High-Energy ' if name.startswith('high_') else 'Hardened ' if name.startswith('hardened_') else 'Precise ') + original
    langs['en_us'][f'item.tacz_bsb.{name}'] = en
    langs['zh_cn'][f'item.tacz_bsb.{name}'] = zh
    return 'tacz_bsb:' + name

def replace(data, mapping):
    if isinstance(data, str): return mapping.get(data, data)
    if isinstance(data, list): return [replace(v, mapping) for v in data]
    if isinstance(data, dict): return {k: replace(v, mapping) for k, v in data.items()}
    return data

high = {}
for base, zh in [('gunpowder_cake','高能火药饼'), ('gunpowder_cake_dry','高能干燥火药饼'),
                 ('gunpowder_cylinder','高能火药柱'), ('gunpowder_charge','高能发射药'),
                 ('gunpowder_pellets','高能火药丸'), ('gunpowder_grains','高能火药颗粒')]:
    high['tacz_c:' + base] = material('high_' + base, 'tacz_c:' + base, zh)
explosive = material('high_explosive', 'minecraft:gunpowder', '高能炸药', 'High-Energy Explosive')
recipes['high_explosive_mixing'] = {
    'type': 'create:mixing',
    'ingredients': [{'item': v} for v in ['create:powdered_obsidian', 'minecraft:blaze_powder',
                                        'minecraft:blaze_powder', 'minecraft:gunpowder', 'minecraft:gunpowder']],
    'results': [{'id': explosive, 'count': 4}]
}
for name in ['gunpowder_cake_mix', 'gunpowder_cake_drying', 'gunpowder_cylinder_compact',
             'gunpowder_pellet_press', 'gunpowder_charge_recipe', 'gunpowder_pellets_recipe_revert',
             'gunpowder_grains_recipe', 'gunpowder_pellets_recipe']:
    d = replace(source[name], high)
    if name == 'gunpowder_cake_mix':
        d['ingredients'] += [{'item': 'create:rose_quartz'}, {'item': 'create:rose_quartz'}]
        d['heat_requirement'] = 'heated'
    recipes['high_' + name] = d

hardened = {}
for base, zh in [('bullet','硬化小型弹头'), ('large_bullet','硬化大型弹头'), ('pellets','硬化金属弹丸')]:
    hardened['tacz_c:' + base] = material('hardened_' + base, 'tacz_c:' + base, zh)
    recipes['hardened_' + base + '_smelting'] = {'type': 'minecraft:smelting', 'category': 'misc',
        'ingredient': {'item': 'tacz_c:' + base}, 'result': {'id': hardened['tacz_c:' + base], 'count': 1},
        'experience': 0.1, 'cookingtime': 200}

parts = {}
for base, zh in [('rpg_warhead','精密的RPG-7战斗部'), ('rpg_sustainer_motor','精密的RPG-7发动机'),
                 ('rpg_booster_charge','精密的RPG-7发射药'), ('explosive_charge_40mm','精密的40mm榴弹炸药装药'),
                 ('fuse_40mm','精密的40mm榴弹引信'), ('fuseless_40mm','精密的40mm无引信榴弹')]:
    parts['tacz_c:' + base] = material('precise_' + base, 'tacz_c:' + base, zh)
for name in ['rpg_warhead_recipe', 'rpg_sustainer_motor_recipe', 'rpg_booster_charge_recipe',
             'grenade_explosive_charge_40mm', 'grenade_fuse_40mm']:
    recipes['precise_' + name] = replace(source[name], high | parts | {'minecraft:gunpowder': explosive})

# Dedicated transitions prevent partially processed items from entering another tier's recipe.
# Start charging with high-energy powder, so the deployer's held ingredient selects the tier.
def assembly(original, mapping, transitional, high_first=False):
    d = replace(original, mapping)
    d['transitional_item'] = {'id': transitional}
    for step in d['sequence']:
        step['ingredients'][0] = {'item': transitional}
        step['results'] = [{'id': transitional}]
    if high_first:
        first = next(i for i, step in enumerate(d['sequence'])
                     if len(step['ingredients']) > 1 and step['ingredients'][1].get('item') in high.values())
        d['sequence'].insert(0, d['sequence'].pop(first))
    return d

calibers = sorted(n.removeprefix('bullet_').removesuffix('_cap') for n in source
                  if n.startswith('bullet_') and n.endswith('_cap'))
assert len(calibers) == 22
for caliber in calibers:
    old_case = 'tacz_c:casefull_' + caliber
    display_caliber = re.sub(r'§.', '', base_en['item.tacz_c.casefull_' + caliber])
    display_caliber = display_caliber.removeprefix('Prepared ').removesuffix(' Bullet Casing')
    prepared = material('high_casefull_' + caliber, old_case, f'高能{display_caliber}已装药弹壳')
    charging = material('unfinished_high_casefull_' + caliber,
                        'tacz_c:unfinished_casefull_12g' if caliber == '12g' else old_case,
                        f'加工中的高能{display_caliber}已装药弹壳', f'Unfinished High-Energy {display_caliber} Prepared Casing')
    finishing = material('unfinished_precise_' + caliber, 'tacz_c:unfinished_' + caliber,
                         f'加工中的精密{display_caliber}弹药', f'Unfinished Precise {display_caliber} Ammunition')
    recipes['high_bullet_' + caliber] = assembly(source['bullet_' + caliber],
                                                high | hardened | {old_case: prepared}, charging, high_first=True)
    recipes['precise_bullet_' + caliber + '_cap'] = assembly(source['bullet_' + caliber + '_cap'],
        high | hardened | {old_case: prepared, 'tacz:ammo': 'tacz_bsb:precise_ammo'}, finishing)

transitional = material('unfinished_precise_fuseless_40mm', 'tacz_c:booster_charge_40mm',
                       '加工中的精密40mm无引信榴弹', 'Unfinished Precise Fuseless 40mm Grenade')
recipes['precise_grenade_booster_charge_40mm'] = assembly(source['grenade_booster_charge_40mm'],
                                                        high | parts, transitional, high_first=True)
for name, base, zh in [('rpg_rocket_assembly','unfinished_rpg_rocket','加工中的精密RPG-7火箭弹'),
                       ('grenade_40mm_assembly','unfinished_40mm','加工中的精密40mm榴弹')]:
    transitional = material('unfinished_precise_' + base.removeprefix('unfinished_'), 'tacz_c:' + base,
                           zh, 'Unfinished Precise ' + ('RPG-7 Rocket' if 'rpg' in name else '40mm Grenade'))
    recipes['precise_' + name] = assembly(source[name], parts | {'tacz:ammo': 'tacz_bsb:precise_ammo'}, transitional)

for name, data in recipes.items(): save(RES / f'data/tacz_bsb/recipe/{name}.json', data)
for code, entries in langs.items(): save(RES / f'assets/tacz_bsb/lang/{code}.json', entries)
for tier in ['improved', 'precise']:
    save(RES / f'assets/tacz_bsb/models/item/{tier}_ammo.json', {'parent': 'tacz:item/ammo'})

# Every material, including unfinished assemblies, is visible in the Create TaCZ tab.
lines = '\n'.join(f'        add("{name}");' for name in materials)
(JAVA / 'BsbMaterials.java').write_text('''package com.sange.tacz_bsb;

import com.sange.tacz_bsb.item.GlintMaterialItem;
import com.sange.tacz_bsb.item.GlintAssemblyItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Generated by tools/generate_precise_recipes.py against the pinned Create TaCZ release. */
public final class BsbMaterials {
    private static final Map<String, DeferredItem<? extends Item>> REGISTERED = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = Collections.unmodifiableMap(REGISTERED);
    static {
''' + lines + '''
    }
    private static void add(String name) {
        REGISTERED.put(name, name.startsWith("unfinished_")
                ? BsbContent.ITEMS.register(name, GlintAssemblyItem::new)
                : BsbContent.ITEMS.register(name, GlintMaterialItem::new));
    }
    public static void initialize() { /* Trigger registration before attaching the item register to the bus. */ }
}
''', encoding='utf-8')
print(f'Generated {len(materials)} materials, {len(recipes)} recipes, 24 precise ammunition outputs.')
