from pathlib import Path
from lxml import etree
from docx import Document
import zipfile,hashlib,json
p=Path('output/final-report/2026-Y2-S1-MTR-02_nusla_final.docx');refreshed=Path('tmp/final-report/word-refreshed.docx')
with zipfile.ZipFile(p) as base,zipfile.ZipFile(refreshed) as word:
 parts={n:base.read(n) for n in base.namelist()}
 def rels(z):return {e.get('Id'):(e.get('Type'),e.get('Target'),e.get('TargetMode')) for e in etree.fromstring(z.read('word/_rels/document.xml.rels'))}
 br,wr=rels(base),rels(word);reverse={v:k for k,v in br.items()}
 xml=etree.fromstring(word.read('word/document.xml'))
 rns='http://schemas.openxmlformats.org/officeDocument/2006/relationships'
 for e in xml.iter():
  for a,v in list(e.attrib.items()):
   if a.startswith('{'+rns+'}') and v in wr:
    if wr[v] not in reverse:raise ValueError('New unmapped relationship '+str(wr[v]))
    e.set(a,reverse[wr[v]])
 parts['word/document.xml']=etree.tostring(xml,xml_declaration=True,encoding='UTF-8',standalone=True)
with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED) as z:
 for n,data in parts.items():z.writestr(n,data)
source=Path(r'D:\SLIIT\Year 02\Semester 01\SE\Project\Final Report\2026-Y2-S1-MTR-02_nusla.docx')
with zipfile.ZipFile(source) as s,zipfile.ZipFile(p) as z:
 check={n:s.read(n)==z.read(n) for n in s.namelist() if n.startswith(('word/header','word/footer','word/theme','word/numbering','customXml','word/media'))}
 assert all(check.values()),check
print('Preserved template parts',len(check),'Sections',len(Document(p).sections))
print('Source unchanged',hashlib.sha256(source.read_bytes()).hexdigest())
print('Final files',p.stat().st_size,Path('output/final-report/2026-Y2-S1-MTR-02.pdf').stat().st_size)
