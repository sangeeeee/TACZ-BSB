"""Generate the checked-in precise-ammunition recipes/models from the pinned Create TaCZ 1.0.2 jar.
Usage: python tools/generate_precise_recipes.py path/to/create-tacz.jar
No runtime dependency on this script or on the original jar's filesystem location.
"""
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

def material(name, base, en=None):
    assert name not in materials
    materials[name] = base
    namespace, path = base.split(':')
    assert namespace == 'minecraft' or f'assets/{namespace}/models/item/{path}.json' in z.namelist(), base
    save(RES / f'assets/tacz_bsb/models/item/{name}.json', {'parent': f'{namespace}:item/{path}'})
    if en is None:
        original = re.sub(r'§.', '', base_en[f'item.{namespace}.{path}'])
        en = ('High-Energy ' if name.startswith('high_') else 'Hardened ' if name.startswith('hardened_') else 'Precise ') + original
    langs['en_us'][f'item.tacz_bsb.{name}'] = en
    # Maintain Chinese terminology directly in zh_cn.json.
    assert f'item.tacz_bsb.{name}' in langs['zh_cn'], f'Missing Chinese translation: {name}'
    return 'tacz_bsb:' + name

def replace(data, mapping):
    if isinstance(data, str): return mapping.get(data, data)
    if isinstance(data, list): return [replace(v, mapping) for v in data]
    if isinstance(data, dict): return {k: replace(v, mapping) for k, v in data.items()}
    return data

high = {}
for base in ['gunpowder_cake', 'gunpowder_cake_dry', 'gunpowder_cylinder', 'gunpowder_charge', 'gunpowder_pellets', 'gunpowder_grains']:
    high['tacz_c:' + base] = material('high_' + base, 'tacz_c:' + base)
explosive = material('high_explosive', 'minecraft:gunpowder', 'High-Energy Explosive')
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
        d['ingredients'] += [{'item': 'create:rose_quartz'}]
        d['heat_requirement'] = 'heated'
    recipes['high_' + name] = d

hardened = {}
for base in ['bullet', 'large_bullet', 'pellets']:
    hardened['tacz_c:' + base] = material('hardened_' + base, 'tacz_c:' + base)
    transition = material('unfinished_hardened_' + base, 'tacz_c:' + base, 'Unfinished ' + langs['en_us']['item.tacz_bsb.hardened_' + base])
    sequence = []
    for fluid in ['minecraft:lava', None, 'minecraft:water']:
        ingredients = [{'item': transition}]
        if fluid:
            ingredients.append({'type': 'neoforge:single', 'amount': 100, 'fluid': fluid})
        sequence.append({'type': 'create:filling' if fluid else 'create:pressing',
                         'ingredients': ingredients, 'results': [{'id': transition}]})
    recipes['hardened_' + base + '_assembly'] = {
        'type': 'create:sequenced_assembly', 'ingredient': {'item': 'tacz_c:' + base},
        'transitional_item': {'id': transition}, 'loops': 1, 'sequence': sequence,
        'results': [{'id': hardened['tacz_c:' + base], 'count': 1}]}

parts = {}
for base in ['rpg_warhead', 'rpg_sustainer_motor', 'rpg_booster_charge', 'explosive_charge_40mm', 'fuse_40mm', 'fuseless_40mm']:
    parts['tacz_c:' + base] = material('precise_' + base, 'tacz_c:' + base)
for name in ['rpg_warhead_recipe', 'rpg_sustainer_motor_recipe', 'rpg_booster_charge_recipe',
             'grenade_explosive_charge_40mm', 'grenade_fuse_40mm']:
    recipes['precise_' + name] = replace(source[name], high | parts | {'minecraft:gunpowder': explosive})

# Dedicated transitions prevent partially processed items from entering another tier's recipe.
# Preserve every original operation in place; the deployer event handles shared-prefix branching.
def assembly(original, mapping, transitional):
    d = replace(original, mapping)
    d['transitional_item'] = {'id': transitional}
    for step in d['sequence']:
        step['ingredients'][0] = {'item': transitional}
        step['results'] = [{'id': transitional}]
    return d

calibers = sorted(n.removeprefix('bullet_').removesuffix('_cap') for n in source
                  if n.startswith('bullet_') and n.endswith('_cap'))
assert len(calibers) == 22
for caliber in calibers:
    old_case = 'tacz_c:casefull_' + caliber
    display_caliber = re.sub(r'§.', '', base_en['item.tacz_c.casefull_' + caliber])
    display_caliber = display_caliber.removeprefix('Prepared ').removesuffix(' Bullet Casing')
    prepared = material('high_casefull_' + caliber, old_case)
    charging = material('unfinished_high_casefull_' + caliber,
                        'tacz_c:unfinished_casefull_12g' if caliber == '12g' else old_case, f'Unfinished High-Energy {display_caliber} Prepared Casing')
    finishing = material('unfinished_precise_' + caliber, 'tacz_c:unfinished_' + caliber, f'Unfinished Precise {display_caliber} Ammunition')
    recipes['high_bullet_' + caliber] = assembly(source['bullet_' + caliber],
                                                high | hardened | {old_case: prepared}, charging)
    recipes['precise_bullet_' + caliber + '_cap'] = assembly(source['bullet_' + caliber + '_cap'],
        high | hardened | {old_case: prepared, 'tacz:ammo': 'tacz_bsb:precise_ammo'}, finishing)

transitional = material('unfinished_precise_fuseless_40mm', 'tacz_c:booster_charge_40mm', 'Unfinished Precise Fuseless 40mm Grenade')
recipes['precise_grenade_booster_charge_40mm'] = assembly(source['grenade_booster_charge_40mm'],
                                                        high | parts, transitional)
for name, base in [('rpg_rocket_assembly', 'unfinished_rpg_rocket'), ('grenade_40mm_assembly', 'unfinished_40mm')]:
    transitional = material('unfinished_precise_' + base.removeprefix('unfinished_'), 'tacz_c:' + base, 'Unfinished Precise ' + ('RPG-7 Rocket' if 'rpg' in name else '40mm Grenade'))
    recipes['precise_' + name] = assembly(source[name], parts | {'tacz:ammo': 'tacz_bsb:precise_ammo'}, transitional)

# Hide both our work-in-progress items and the original Create TaCZ unfinished items.
hidden = ['tacz_bsb:' + name for name in materials if name.startswith('unfinished_')]
hidden += ['tacz_c:' + name.removeprefix('item.tacz_c.') for name in base_en
           if name.startswith('item.tacz_c.unfinished_')]
save(RES / 'data/c/tags/item/hidden_from_recipe_viewers.json', {'replace': False, 'values': hidden})

pairs = [('tacz_c:bullet_' + c, 'tacz_bsb:high_bullet_' + c) for c in calibers]
pairs.append(('tacz_c:grenade_booster_charge_40mm', 'tacz_bsb:precise_grenade_booster_charge_40mm'))
entries = ',\n'.join(f'            Map.entry(ResourceLocation.parse("{a}"), ResourceLocation.parse("{b}"))' for a,b in pairs)
(JAVA / 'recipe/AssemblyPairs.java').write_text('package com.sange.tacz_bsb.recipe;\n\nimport net.minecraft.resources.ResourceLocation;\nimport java.util.Map;\n\n/** Generated default production families; unrelated datapack recipes are never interchangeable. */\npublic final class AssemblyPairs {\n    public static final Map<ResourceLocation, ResourceLocation> PAIRS = Map.ofEntries(\n' + entries + ');\n    public static ResourceLocation other(ResourceLocation id) {\n        ResourceLocation direct = PAIRS.get(id);\n        if (direct != null) return direct;\n        for (var entry : PAIRS.entrySet()) if (entry.getValue().equals(id)) return entry.getKey();\n        return null;\n    }\n}\n', encoding='utf-8')

for name, data in recipes.items(): save(RES / f'data/tacz_bsb/recipe/{name}.json', data)
for code, entries in langs.items(): save(RES / f'assets/tacz_bsb/lang/{code}.json', entries)
for tier in ['improved', 'precise']:
    save(RES / f'assets/tacz_bsb/models/item/{tier}_ammo.json', {'parent': 'tacz:item/ammo'})

# Only completed materials are listed in creative tabs; unfinished items remain registered for processing.
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
