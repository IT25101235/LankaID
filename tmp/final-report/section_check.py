from docx import Document
from docx.oxml.ns import qn
from pathlib import Path
p=Path('output/final-report/2026-Y2-S1-MTR-02_nusla_final.docx')
d=Document(p)
for s in d._element.xpath('.//w:sectPr'):
 print(s.getparent().getparent().tag.split('}')[-1],s.getparent().getparent().getparent().tag.split('}')[-1],s.get(qn('w:rsidR')))
print('sdt sects',len(d._element.xpath('.//w:sdt//w:sectPr')))
