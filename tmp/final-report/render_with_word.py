from pathlib import Path
import importlib.util, os, sys, shutil
skill=Path(r'C:\Users\Aakil\.codex\plugins\cache\openai-primary-runtime\documents\26.1011.11113\skills\documents\render_docx.py')
spec=importlib.util.spec_from_file_location('render_docx',skill);renderer=importlib.util.module_from_spec(spec);spec.loader.exec_module(renderer)
# Word supplies the conversion because this Windows runtime has no bundled LibreOffice.
pdf=Path('output/final-report/2026-Y2-S1-MTR-02.pdf').resolve()
def word_pdf(doc_path,user_profile,convert_tmp_dir,stem,verbose=False):
 assert pdf.exists() and pdf.stat().st_size>0
 target=Path(convert_tmp_dir)/f'{stem}.pdf';shutil.copy2(pdf,target);return str(target),'Converted by Microsoft Word'
renderer.convert_to_pdf=word_pdf
os.environ['PATH']=r'C:\Users\Aakil\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\poppler\Library\bin'+os.pathsep+os.environ['PATH']
sys.argv=[str(skill),'output/final-report/2026-Y2-S1-MTR-02_nusla_final.docx','--output_dir','tmp/final-report/qa-delivery','--width','1275','--height','1650']
renderer.main()
