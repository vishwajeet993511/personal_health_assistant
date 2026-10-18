"""Create a labeled contact sheet for visual QA; never changes source images."""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
poses=json.loads((root/'src/app/src/main/assets/program_poses.json').read_text())
start,end=int(sys.argv[1]),int(sys.argv[2])
premium=len(sys.argv)>3 and sys.argv[3]=='premium'
rows=poses[start:end]
w,h=224,248
sheet=Image.new('RGB',(w*6,h*((len(rows)+5)//6)),'#ecece7')
draw=ImageDraw.Draw(sheet)
for i,p in enumerate(rows):
    key=p['image']+('_premium' if premium else '')
    paths=[root/'src/app/src/main/res/drawable-nodpi'/(key+ext) for ext in ['.webp','.png']]
    path=next((x for x in paths if x.exists()),None)
    x,y=(i%6)*w,(i//6)*h
    if path:
        with Image.open(path) as im:
            im=im.convert('RGB'); im.thumbnail((w-12,h-48))
            sheet.paste(im,(x+(w-im.width)//2,y+(h-48-im.height)//2))
    label=f'{start+i}: {p["name"]}'
    draw.text((x+6,y+h-44),label[:31],fill='black')
    draw.text((x+6,y+h-28),label[31:62],fill='black')
path=Path('/tmp')/f'pose-review-{start}-{end}{"-premium" if premium else ""}.jpg'
sheet.save(path,quality=92)
print(path)
