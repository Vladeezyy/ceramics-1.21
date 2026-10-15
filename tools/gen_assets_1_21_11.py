#!/usr/bin/env python3
"""
One-time conversion of the 1.21.1 assets for Minecraft 1.21.11.

* Cracks: the "ceramics:cracked" geometry loader is gone. Cracks are the block state property "cracks" and the
  crack overlay is a plain model drawn over the block, like the grass overlay. For every model with the loader this
  writes <model>_cracks.json (the same elements textured "#cracks") and <model>_cracks_<1-5>.json (the textures),
  adds the overlay parts to the block states, and marks the models cutout.
* Items: every item gets a client item definition in assets/ceramics/items, crackable items select the overlay on
  the ceramics:cracks component, clay buckets use the ceramics:clay_bucket item model.
* Armor: the clay armor textures move to the equipment asset ceramics:clay.

Run from the repository root: python3 tools/gen_assets_1_21_11.py
"""
import json
import os
import shutil

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'ceramics')
MODELS = os.path.join(ROOT, 'models')
CRACKS = 5
CUTOUT = 'minecraft:cutout'


def model_path(name):
  """ceramics:block/x -> path, None for other namespaces"""
  if ':' in name:
    namespace, path = name.split(':', 1)
  else:
    namespace, path = 'minecraft', name
  if namespace != 'ceramics':
    return None
  return os.path.join(MODELS, path + '.json')


def load(name):
  path = model_path(name)
  if path is None or not os.path.exists(path):
    return None
  with open(path) as f:
    return json.load(f)


def save(path, data):
  os.makedirs(os.path.dirname(path), exist_ok=True)
  with open(path, 'w') as f:
    json.dump(data, f, indent=2)
    f.write('\n')


def loader_model(name):
  """The model in the parent chain that has the cracked loader, or None"""
  while name is not None:
    data = load(name)
    if data is None:
      return None
    if data.get('loader') == 'ceramics:cracked':
      return name
    name = data.get('parent')
    if name is not None and ':' not in name:
      name = 'minecraft:' + name
  return None


def elements(name):
  """Elements of the model, from the closest parent defining them"""
  while name is not None:
    data = load(name)
    if data is None:
      raise ValueError('No elements for ' + name)
    if 'elements' in data:
      return data['elements']
    name = data.get('parent')
  raise ValueError('No elements')


def overlay_elements(source):
  """Copies the elements, each face textured with the cracks and untinted"""
  result = []
  for element in source:
    copy = {key: value for key, value in element.items() if key not in ('faces', 'name')}
    faces = {}
    for side, face in element['faces'].items():
      face = {key: value for key, value in face.items() if key != 'tintindex'}
      face['texture'] = '#cracks'
      faces[side] = face
    copy['faces'] = faces
    result.append(copy)
  return result


def convert_cracked_models():
  """Removes the loader and writes the overlay models, returns the converted model names"""
  converted = []
  for folder, _, files in os.walk(MODELS):
    for file in files:
      path = os.path.join(folder, file)
      with open(path) as f:
        data = json.load(f)
      if data.get('loader') != 'ceramics:cracked':
        continue
      name = 'ceramics:' + os.path.relpath(path, MODELS)[:-5].replace(os.sep, '/')
      textures = data.get('textures', {})
      crack_textures = [textures.pop('cracks_%d' % i) for i in range(1, CRACKS + 1)]
      del data['loader']
      data['render_type'] = CUTOUT
      if not textures:
        data.pop('textures', None)
      save(path, data)
      # overlay geometry, parented to the model for the display transforms
      save(path[:-5] + '_cracks.json', {
        'parent': name,
        'render_type': CUTOUT,
        'elements': overlay_elements(elements(name)),
      })
      for i, texture in enumerate(crack_textures, 1):
        save(path[:-5] + '_cracks_%d.json' % i, {
          'parent': name + '_cracks',
          'textures': {'cracks': texture, 'particle': texture},
        })
      converted.append(name)
  return converted


def convert_blockstates():
  """Adds the crack overlays to block states using cracked models"""
  folder = os.path.join(ROOT, 'blockstates')
  for file in sorted(os.listdir(folder)):
    path = os.path.join(folder, file)
    with open(path) as f:
      data = json.load(f)
    # variants to multipart, so the overlay can be another part
    if 'variants' in data:
      models = [variant['model'] for variant in data['variants'].values() if isinstance(variant, dict)]
      if not any(loader_model(model) for model in models):
        continue
      parts = []
      for key, variant in data['variants'].items():
        when = dict(pair.split('=') for pair in key.split(',')) if key else {}
        part = {'apply': variant}
        if when:
          part['when'] = when
        parts.append(part)
      data = {'multipart': parts}
    if 'multipart' not in data:
      continue
    parts = data['multipart']
    overlays = []
    for part in parts:
      apply = part['apply']
      if isinstance(apply, list):
        if any(loader_model(model['model']) for model in apply):
          raise ValueError('Weighted cracked parts are not supported: ' + file)
        continue
      geometry = loader_model(apply['model'])
      if geometry is None:
        continue
      for i in range(1, CRACKS + 1):
        when = dict(part.get('when', {}))
        if 'OR' in when or 'AND' in when:
          raise ValueError('OR conditions are not supported: ' + file)
        when['cracks'] = str(i)
        overlay = dict(apply)
        overlay['model'] = '%s_cracks_%d' % (geometry, i)
        overlays.append({'when': when, 'apply': overlay})
    if overlays:
      data['multipart'] = parts + overlays
      save(path, data)
      print('blockstate', file, len(overlays), 'overlay parts')


def plain(model):
  return {'type': 'minecraft:model', 'model': model}


def write_items():
  items = os.path.join(ROOT, 'items')
  item_models = os.path.join(MODELS, 'item')
  buckets = {
    'fluid_clay_bucket': ('fluid', 'normal'),
    'cracked_fluid_clay_bucket': ('fluid', 'cracked'),
    'solid_clay_bucket': ('block', 'normal'),
    'cracked_solid_clay_bucket': ('block', 'cracked'),
  }
  for file in sorted(os.listdir(item_models)):
    item = file[:-5]
    # crack overlays of item models are no items
    if '_cracks' in item:
      continue
    model = 'ceramics:item/' + item
    if item in buckets:
      contents, style = buckets[item]
      definition = {
        'type': 'ceramics:clay_bucket',
        'contents': contents,
        'textures': {
          'base': 'ceramics:item/clay_bucket/%s_cover' % style,
          'inner': 'ceramics:item/clay_bucket/%s_inner' % style,
          'fluid': 'ceramics:item/clay_bucket/%s_fluid' % style,
        },
      }
      # the geometry loader model is no longer used
      os.remove(os.path.join(item_models, file))
    else:
      geometry = loader_model(model)
      if geometry is None:
        definition = plain(model)
      else:
        definition = {
          'type': 'minecraft:select',
          'property': 'minecraft:component',
          'component': 'ceramics:cracks',
          'cases': [
            {'when': i, 'model': {'type': 'minecraft:composite', 'models': [plain(model), plain('%s_cracks_%d' % (geometry, i))]}}
            for i in range(1, CRACKS + 1)
          ],
          'fallback': plain(model),
        }
    save(os.path.join(items, item + '.json'), {'model': definition})


def convert_armor():
  old = os.path.join(ROOT, 'textures', 'models', 'armor')
  if not os.path.isdir(old):
    return
  equipment = os.path.join(ROOT, 'textures', 'entity', 'equipment')
  for layer, folder in (('clay_layer_1.png', 'humanoid'), ('clay_layer_2.png', 'humanoid_leggings')):
    target = os.path.join(equipment, folder, 'clay.png')
    os.makedirs(os.path.dirname(target), exist_ok=True)
    shutil.move(os.path.join(old, layer), target)
  shutil.rmtree(os.path.join(ROOT, 'textures', 'models'))
  save(os.path.join(ROOT, 'equipment', 'clay.json'), {
    'layers': {
      'humanoid': [{'texture': 'ceramics:clay'}],
      'humanoid_leggings': [{'texture': 'ceramics:clay'}],
    }
  })


def main():
  # block states need the loaders to find the geometry, so read them before converting the models
  convert_blockstates()
  converted = convert_cracked_models()
  print('cracked models', len(converted))
  # items last: the cracked item models are known by their overlay files now
  write_items_after_models()
  convert_armor()


def write_items_after_models():
  # loader_model looks for the loader key, which is gone now; use the overlay files instead
  global loader_model
  def overlay_owner(name):
    while name is not None:
      path = model_path(name)
      if path is None or not os.path.exists(path):
        return None
      if os.path.exists(path[:-5] + '_cracks.json'):
        return name
      with open(path) as f:
        name = json.load(f).get('parent')
    return None
  loader_model = overlay_owner
  write_items()


if __name__ == '__main__':
  main()
