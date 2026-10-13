from pathlib import Path
from docx import Document
from lxml import etree
from pypdf import PdfReader
import re, json
p=Path('tmp/final-report')
sql=Path('database/current-database-schema.sql').read_text(encoding='utf-8-sig')
print('SCHEMA',len(re.findall(r'CREATE TABLE',sql)),len(re.findall(r'FOREIGN KEY',sql)))
doc=Document(r'D:\SLIIT\Year 02\Semester 01\SE\Project\Final Report\2026-Y2-S1-MTR-02_nusla.docx')
print('runs',[(i,[(r.text,r.font.name,r.font.size.pt if r.font.size else None) for r in doc.paragraphs[i].runs]) for i in [2,3,18,22]])
print('children',[(i,e.tag.split('}')[-1],''.join(e.itertext())[:35]) for i,e in enumerate(doc._element.body)][:24])
print('section nums',[(i,s._sectPr.xml[-650:]) for i,s in enumerate(doc.sections)])
print('counts',len(list(Path('src/main/java').rglob('*.java'))),len(list(Path('src/test/java').rglob('*Test*.java'))))

