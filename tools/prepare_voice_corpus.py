#!/usr/bin/env python3
"""Export the current catalog's full set of reachable utterances for synthesis."""
import pathlib,subprocess,sys
root=pathlib.Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'import_osaka_content.py')],check=True)
java=root/'app/src/main/java/kr/travel/hitokoto';classes=root/'build/voice-export';classes.mkdir(parents=True,exist_ok=True)
subprocess.run(['javac','-encoding','UTF-8','--release','11','-d',str(classes),*[str(java/(name+'.java')) for name in ('Phrase','Quantity','Catalog','OsakaContent')],str(root/'tools/ExportVoice.java')],check=True)
corpus=subprocess.check_output(['java','-cp',str(classes),'kr.travel.hitokoto.ExportVoice'])
output=root/'build/voice-corpus.b64';output.write_bytes(corpus);print(output)
