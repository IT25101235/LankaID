from pathlib import Path
from PIL import Image,ImageDraw
from pypdf import PdfReader
import zipfile
p=Path('tmp/final-report/qa-render');files=sorted(p.glob('page-*.png'),key=lambda p:int(p.stem.split('-')[-1]))
for group in range((len(files)+7)//8):
 im=Image.new('RGB',(1200,820),'#ccc');d=ImageDraw.Draw(im)
 for i,f in enumerate(files[group*8:(group+1)*8]):
  t=Image.open(f);t.thumbnail((290,380));x=i%4*300;y=i//4*410;im.paste(t,(x,y+22));d.text((x+5,y+3),f.stem,fill='black')
 im.save(p/f'contact-{group+1}.jpg')
base=zipfile.ZipFile('output/final-report/2026-Y2-S1-MTR-02_nusla_final.docx');upd=zipfile.ZipFile('tmp/final-report/word-refreshed.docx')
print('Changed members', [n for n in base.namelist() if n in upd.namelist() and base.read(n)!=upd.read(n)])
