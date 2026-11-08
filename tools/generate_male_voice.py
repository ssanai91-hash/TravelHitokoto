#!/usr/bin/env python3
"""Offline build asset generation; VOICEVOX 0.17.1, Kurono Takehiro normal style 11.
The VVM/core/dictionary are build dependencies only; APK contains Ogg clips.
Input corpus comes from ExportVoice.java; never edit corpus by hand.
"""
import argparse, base64, concurrent.futures, hashlib, json, logging, os, subprocess, time
from pathlib import Path
from voicevox_core.blocking import Onnxruntime, OpenJtalk, Synthesizer, VoiceModelFile
synth=None
output=Path(__file__).resolve().parents[1]/'app/src/main/assets/voice/male'
def initialize(runtime, model_path):
    global synth
    logging.basicConfig(level=logging.ERROR)
    runtime=Path(runtime)
    ort=Onnxruntime.load_once(filename=str(next(runtime.rglob('libvoicevox_onnxruntime.so.*'))))
    synth=Synthesizer(ort,OpenJtalk(str(next(runtime.glob('open_jtalk_dic*')))),acceleration_mode='CPU',cpu_num_threads=1)
    with VoiceModelFile.open(model_path) as model:synth.load_voice_model(model)
    assert any(m.name=='玄野武宏' and any(s.id==11 for s in m.styles) for m in synth.metas())
def create(text):
    key=hashlib.sha256(text.encode()).hexdigest();dest=output/(key+'.ogg')
    if dest.exists() and dest.stat().st_size>100:return key
    wav=synth.tts(text,11)
    part=output/(key+'.part')
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-threads','1','-i','pipe:0','-ac','1','-c:a','libvorbis','-q:a','2','-f','ogg','-y',str(part)],input=wav,check=True)
    part.replace(dest);return key
def main():
    p=argparse.ArgumentParser();p.add_argument('--runtime',type=Path,required=True);p.add_argument('--model',type=Path,required=True);p.add_argument('--corpus',type=Path,required=True);p.add_argument('--workers',type=int,default=4);a=p.parse_args()
    root=Path(__file__).resolve().parents[1];output.mkdir(parents=True,exist_ok=True)
    texts=[base64.b64decode(s).decode('utf-8') for s in a.corpus.read_text().splitlines() if s]
    start=time.time();manifest=[]
    with concurrent.futures.ProcessPoolExecutor(max_workers=a.workers,initializer=initialize,initargs=(str(a.runtime),str(a.model))) as pool:
        for i,key in enumerate(pool.map(create,texts),1):
            manifest.append({'sha256':key,'text':texts[i-1]})
            if i%25==0 or i==len(texts): print(f'{i}/{len(texts)} clips; {time.time()-start:.1f}s',flush=True)
    (root/'voice-corpus.json').write_text(json.dumps({'voice':'VOICEVOX:玄野武宏','style':11,'core_version':'0.17.1','clips':manifest},ensure_ascii=False,indent=2)+'\n')
    print(f'Finished: {len(texts)} clips, {sum(f.stat().st_size for f in output.glob("*.ogg"))/1048576:.1f} MiB',flush=True)
if __name__=='__main__':main()
