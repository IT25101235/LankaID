from pathlib import Path
from PIL import Image,ImageDraw
from docx import Document
from lxml import etree
import json, xml.etree.ElementTree as ET
out=Path('tmp/final-report')
imgs=sorted((out/'yasiru_media').glob('*'),key=lambda p:int(p.stem.replace('image','')))
sheet=Image.new('RGB',(1400,((len(imgs)+3)//4)*260),'white');d=ImageDraw.Draw(sheet)
for i,p in enumerate(imgs):
 im=Image.open(p);print(p.name,im.size)
 im.thumbnail((330,225));x=(i%4)*350;y=(i//4)*260
 sheet.paste(im,(x,y+25));d.text((x+5,y+5),f'{p.name}',fill='black')
sheet.save(out/'assets-contact.jpg')
doc=Document(r'D:\SLIIT\Year 02\Semester 01\SE\Project\Final Report\SE2030-Final-Report-2026-Y2-S1-MTR-02-yasiru.docx')
for p in doc.paragraphs:
 for blip in p._p.xpath('.//a:blip'):
  rid=blip.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}embed');print('IMG',doc.part.related_parts[rid].partname)
root=Path('.')
files=list((root/'target/surefire-reports').glob('TEST-*.xml'))
metrics={'suites':len(files),'tests':0,'errors':0,'failures':0,'skipped':0,'time':0.0}
for f in files:
 s=ET.parse(f).getroot()
 for k in ['tests','errors','failures','skipped']:metrics[k]+=int(s.get(k,'0'))
 metrics['time']+=float(s.get('time','0'))
print('TEST RESULTS',metrics)
(out/'test_metrics.json').write_text(json.dumps(metrics))
