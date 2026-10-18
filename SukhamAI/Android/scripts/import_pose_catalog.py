"""Import public Pocket Yoga pose metadata and illustrations for the local library.
Source: https://www.pocketyoga.com/pose/ — artwork © Rainfrog, LLC.
Run with a downloaded /poses.json file as the first argument.
"""
import concurrent.futures
import json
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1] / 'src/app/src/main'
images = root / 'res/drawable-nodpi'
images.mkdir(parents=True, exist_ok=True)
source = json.loads(Path(sys.argv[1]).read_text())
categories = {'standing': 'Standing', 'seated': 'Seated', 'supine': 'Supine', 'prone': 'Prone', 'arm_leg_support': 'Arm & leg support', 'arm_balance_and_inversion': 'Balances & inversions'}

def import_pose(pose):
    key = pose['name'].replace(' ', '')
    side = pose.get('preferred_side')
    suffix = '_' + side[0].upper() if side and (pose['two_sided'] or pose['sideways']) else ''
    url = 'https://pocketyoga.com/assets/images/full/' + key + suffix + '.png'
    resource = 'program_pose_' + key.lower()
    path = images / (resource + '.png')
    if not path.exists():
        subprocess.run(['curl', '-f', '-sS', '-L', '--retry', '2', '--max-time', '45', url, '-o', str(path)], check=True)
        subprocess.run(['sips', '-Z', '560', str(path)], check=True, stdout=subprocess.DEVNULL)
    sanskrit = pose.get('sanskrit_names') or []
    return {'id': key, 'name': pose['display_name'], 'sanskrit': sanskrit[0]['latin'] if sanskrit else '', 'category': categories[pose['category']], 'difficulty': pose['difficulty'].title(), 'image': resource, 'sourceUrl': 'https://www.pocketyoga.com/pose/' + key}

with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool:
    catalog = list(pool.map(import_pose, source))
catalog.sort(key=lambda p: p['name'].lower())
(root / 'assets/program_poses.json').write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + '\n')
print(f'Imported {len(catalog)} poses with illustrations.')
# Explicit resource references keep every illustration reachable during resource shrinking.
map_source = 'package com.google.ai.edge.gallery.ui.programs\n\nimport com.google.ai.edge.gallery.R\n\ninternal val programPoseImages = mapOf(\n'
map_source += ''.join('  "' + pose['image'] + '" to R.drawable.' + (pose['image'] + '_premium' if any((images / (pose['image'] + '_premium' + extension)).exists() for extension in ('.png', '.webp')) else pose['image']) + ',\n' for pose in catalog)
map_source += ')\n'
(root / 'java/com/google/ai/edge/gallery/ui/programs/ProgramPoseImages.kt').write_text(map_source)
