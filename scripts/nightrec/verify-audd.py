import json,urllib.request,urllib.parse,time
from pathlib import Path
v={}
for line in Path('local.properties').read_text().splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,x=line.split('=',1);v[k.strip()]=x.strip()
token=v.get('AUDD_API_TOKEN','')
if not token: raise SystemExit('Token missing')
p=Path('docs/verification/audd-request-budget.json')
b=json.loads(p.read_text()) if p.exists() else {'attempted':0,'limit':int(v.get('AUDD_REQUEST_LIMIT','300'))}
if b['attempted']>=b['limit']:raise SystemExit('Local budget exhausted')
b['attempted']+=1;p.write_text(json.dumps(b,indent=2)+'\n')
req=urllib.request.Request('https://api.audd.io/',data=urllib.parse.urlencode({'api_token':token,'url':'https://audd.tech/example.mp3'}).encode())
try:
 with urllib.request.urlopen(req,timeout=45) as r: data=json.load(r)
 result=data.get('result') or {}
 out={'status':data.get('status'),'matched':bool(result),'title':result.get('title'),'artist':result.get('artist'),'request_count':b['attempted'],'verified_at_epoch':int(time.time())}
 if data.get('status')!='success':out['error_code']=(data.get('error') or {}).get('error_code')
 Path('docs/verification/audd-auth.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(out,ensure_ascii=False))
 if out['status']!='success' or not out['matched']:raise SystemExit(1)
except Exception as e:
 print('AudD verification failed:',type(e).__name__);raise SystemExit(1)
