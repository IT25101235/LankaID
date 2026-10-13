from PIL import Image,ImageDraw
from pathlib import Path
p=Path('tmp/final-report/reference-render');files=sorted(p.glob('page*.jpg')); im=Image.new('RGB',(1000,300*((len(files)+3)//4)),'#ddd');d=ImageDraw.Draw(im)
for i,f in enumerate(files):
 t=Image.open(f);t.thumbnail((240,275));x=i%4*250;y=i//4*300;im.paste(t,(x,y+20));d.text((x,y),f.stem,fill='black')
im.save(p/'contact.jpg')
