#!/usr/bin/env python3
"""Restore exact binary resources from the original complete source release.
No API token is stored. Optional download uses the user's authenticated gh CLI.
"""
import hashlib,json,pathlib,shutil,subprocess,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
manifest=json.loads((root/'resource-manifest.json').read_text())
archive=pathlib.Path(sys.argv[1]).expanduser().resolve() if len(sys.argv)>1 else root/'.release-cache'/manifest['archive_name']
if not archive.is_file():
    if len(sys.argv)>1:raise SystemExit('Archive not found: '+str(archive))
    if not shutil.which('gh'):raise SystemExit('Download '+manifest['archive_name']+' from GitHub Releases and pass its path to this script, or install/authenticate GitHub CLI.')
    archive.parent.mkdir(exist_ok=True)
    subprocess.run(['gh','release','download',manifest['resource_release'],'--repo',manifest['repository'],'--pattern',manifest['archive_name'],'--dir',str(archive.parent)],check=True)
def digest(file):
    h=hashlib.sha256()
    with file.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1048576),b''):h.update(chunk)
    return h.hexdigest()
if digest(archive)!=manifest['archive_sha256']:raise SystemExit('Source archive SHA-256 mismatch; restore stopped.')
with zipfile.ZipFile(archive) as source:
    for item in manifest['files']:
        destination=(root/item['path']).resolve()
        if not destination.is_relative_to(root):raise SystemExit('Unsafe resource path.')
        if destination.is_file() and destination.stat().st_size==item['size'] and digest(destination)==item['sha256']:continue
        member='travel-hitokoto/'+item['path']
        if source.getinfo(member).file_size!=item['size']:raise SystemExit('Resource size mismatch: '+item['path'])
        data=source.read(member)
        if hashlib.sha256(data).hexdigest()!=item['sha256']:raise SystemExit('Resource SHA-256 mismatch: '+item['path'])
        destination.parent.mkdir(parents=True,exist_ok=True)
        temporary=destination.with_name(destination.name+'.restore-part')
        temporary.write_bytes(data);temporary.replace(destination)
print('Restored '+str(len(manifest['files']))+' verified resources. Source files were preserved.')
