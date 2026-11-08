#!/usr/bin/env python3
"""Build gate: every reachable phrase must have its fixed male audio asset."""
import base64,hashlib,os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
classes=root/'build/classes';out=root/'build/voice-export';out.mkdir(exist_ok=True)
subprocess.run(['javac','-encoding','UTF-8','--release','11','-cp',str(classes),'-d',str(out),str(root/'tools/ExportVoice.java')],check=True)
corpus=subprocess.check_output(['java','-cp',str(out)+os.pathsep+str(classes),'kr.travel.hitokoto.ExportVoice'],text=True)
missing=[];count=0
for line in corpus.splitlines():
    text=base64.b64decode(line);key=hashlib.sha256(text).hexdigest();file=root/'app/src/main/assets/voice/male'/(key+'.ogg');count+=1
    if not file.is_file() or file.stat().st_size<100:missing.append(text.decode())
if missing:raise SystemExit(f'Missing fixed male audio for {len(missing)} phrases. Regenerate with tools/generate_male_voice.py. First: {missing[0]}')
print(f'Bundled male audio: {count} / {count} phrases')
