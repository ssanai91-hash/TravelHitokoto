#!/usr/bin/env python3
"""Dependency-free Android SDK build. Set ANDROID_SDK_ROOT; JDK 17+ required.
Signs with a persistent LOCAL TEST key, not a Play Store release identity.
"""
import os, pathlib, shutil, subprocess, zipfile, sys
P=pathlib.Path(__file__).resolve().parent
sdk=pathlib.Path(os.environ.get('ANDROID_SDK_ROOT',os.environ.get('ANDROID_HOME','')))
if not (sdk/'platforms/android-35/android.jar').exists():
    raise SystemExit('Install SDK platforms;android-35 and build-tools;35.0.0, then set ANDROID_SDK_ROOT.')
bt=sdk/'build-tools/35.0.0'; android=sdk/'platforms/android-35/android.jar'
out=P/'build'; out.mkdir(exist_ok=True)
def run(*args): subprocess.run([str(x) for x in args],check=True,cwd=P)
for abi in ['arm64-v8a','x86_64']:
    for filename in ['libvoicevox_core.so','libvoicevox_onnxruntime.so','libc++_shared.so','libjnidispatch.so']:
        if not (P/'app/src/main/jniLibs'/abi/filename).is_file():
            raise SystemExit('Missing free-input native dependency: '+abi+'/'+filename)
run(sys.executable,P/'import_osaka_content.py')
classes=out/'classes'
if classes.exists(): shutil.rmtree(classes)
classes.mkdir()
sources=sorted((P/'app/src/main/java').rglob('*.java'))
libraries=sorted((P/'app/libs').glob('*.jar'))
classpath=os.pathsep.join(str(x) for x in [android,*libraries])
run('javac','-encoding','UTF-8','--release','11','-classpath',classpath,'-d',classes,*sources)
run(sys.executable,P/'tools/check_voice_assets.py')
compiled=out/'resources.zip'
run(bt/'aapt2','compile','--dir',P/'app/src/main/res','-o',compiled)
base=out/'base.apk'
run(bt/'aapt2','link','-o',base,'--manifest',P/'app/src/main/AndroidManifest.xml','-I',android,'--min-sdk-version','26','--target-sdk-version','35','--version-code','8','--version-name','0.1.7','-A',P/'app/src/main/assets','-0','ogg',compiled)
jar=out/'classes.jar'
with zipfile.ZipFile(jar,'w') as z:
    for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
run(bt/'d8','--lib',android,'--min-api','26','--output',out,jar,*libraries)
with zipfile.ZipFile(base,'a',zipfile.ZIP_DEFLATED) as z:
    for dex in sorted(out.glob('classes*.dex')):z.write(dex,dex.name)
    for native in sorted((P/'app/src/main/jniLibs').rglob('*.so')):z.write(native,pathlib.Path('lib')/native.relative_to(P/'app/src/main/jniLibs'))
aligned=out/'aligned.apk'; run(bt/'zipalign','-f','-p','4',base,aligned)
key=P/'signing/travel-test.jks'; key.parent.mkdir(exist_ok=True)
if not key.exists():
    run('keytool','-genkeypair','-keystore',key,'-storepass','travel-test','-keypass','travel-test','-alias','travel-test','-dname','CN=Travel Hitokoto Local Test','-keyalg','RSA','-keysize','2048','-validity','10000')
apk=out/'TravelHitokoto-0.1.7.apk'
run(bt/'apksigner','sign','--ks',key,'--ks-pass','pass:travel-test','--key-pass','pass:travel-test','--out',apk,aligned)
run(bt/'apksigner','verify','--verbose',apk)
print(apk)
