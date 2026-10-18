"""Encode generated pose assets for Android; full-resolution originals stay in imagegen output."""
from pathlib import Path
from PIL import Image
images = Path(__file__).resolve().parents[1] / 'src/app/src/main/res/drawable-nodpi'
count = 0
for source in images.glob('program_pose_*_premium.png'):
    # Preserve the original approved PNG as the reusable style reference.
    if source.name == 'program_pose_anklestretch_premium.png':
        continue
    target = source.with_suffix('.webp')
    with Image.open(source) as image:
        image.thumbnail((840, 840), Image.Resampling.LANCZOS)
        image.convert('RGB').save(target, 'WEBP', quality=92, method=6)
    with Image.open(target) as check:
        check.verify()
    source.unlink()
    count += 1
print(f'Optimized {count} generated images for Android.')
