import json,time,urllib.request,uuid
from pathlib import Path
v=dict(line.split('=',1) for line in Path('local.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
p=Path('docs/verification/audd-request-budget.json');b=json.loads(p.read_text());assert b['attempted']<b['limit'];b['attempted']+=1;p.write_text(json.dumps(b,indent=2))
clip=Path('/tmp/nightrec-audd-official-source.mp3').read_bytes();boundary='NightRec'+uuid.uuid4().hex
body=(f'--{boundary}\r\nContent-Disposition: form-data; name="api_token"\r\n\r\n{v["AUDD_API_TOKEN"]}\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="example.mp3"\r\nContent-Type: audio/mpeg\r\n\r\n').encode()+clip+f'\r\n--{boundary}--\r\n'.encode()
request=urllib.request.Request('https://api.audd.io/',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
try:
 with urllib.request.urlopen(request,timeout=45) as response:j=json.load(response)
 result=j.get('result') or {}
 out={'status':j.get('status'),'matched':bool(result),'title':result.get('title'),'artist':result.get('artist'),'inputBytes':len(clip),'hostRequests':b['attempted'],'at':int(time.time())}
 Path('docs/verification/audd-upload.json').write_text(json.dumps(out,ensure_ascii=False,indent=2));print(json.dumps(out,ensure_ascii=False))
except Exception as e:print('Upload failure:',type(e).__name__);raise SystemExit(1)
