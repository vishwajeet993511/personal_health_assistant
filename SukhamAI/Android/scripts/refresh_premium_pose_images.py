"""Refresh explicit image references and resumable redesign progress after asset review."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = root / 'src/app/src/main'
poses = json.loads((main/'assets/program_poses.json').read_text())
images = main/'res/drawable-nodpi'
lines = ['package com.google.ai.edge.gallery.ui.programs', '', 'import com.google.ai.edge.gallery.R', '', 'internal val programPoseImages = mapOf(']
progress = []
for pose in poses:
    key = pose['image']
    premium = any((images/(key+'_premium'+extension)).exists() for extension in ('.png', '.webp'))
    resource = key+'_premium' if premium else key
    lines.append(f'  "{key}" to R.drawable.{resource},')
    progress.append(dict(id=pose['id'], name=pose['name'], resource=key, status='complete' if premium else 'pending'))
lines.append(')')
(main/'java/com/google/ai/edge/gallery/ui/programs/ProgramPoseImages.kt').write_text('\n'.join(lines)+'\n')
(root/'docs/pose-redesign/progress.json').write_text(json.dumps(progress,indent=2)+'\n')
(root/'docs/pose-redesign/remaining.json').write_text(json.dumps([pose for pose in progress if pose['status'] == 'pending'], indent=2)+'\n')
print(f'{sum(p["status"] == "complete" for p in progress)} / {len(progress)} premium illustrations wired.')
