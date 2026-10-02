"""Offline model evidence only: controlled samples are not field quality acceptance."""
import csv, hashlib, json, time, wave
from pathlib import Path
import numpy as np
from ai_edge_litert.interpreter import Interpreter

root = Path(__file__).resolve().parents[2]
model = root / 'app/src/main/assets/yamnet.tflite'
interpreter = Interpreter(model_path=str(model), num_threads=2)
interpreter.allocate_tensors()
input_index = interpreter.get_input_details()[0]['index']
output_index = interpreter.get_output_details()[0]['index']
classes = list(csv.DictReader((model.parent / 'yamnet_class_map.csv').open()))

def classify(waveform):
    records = []
    for offset in range(0, len(waveform) - 15600 + 1, 15600):
        stereo = waveform[offset:offset+15600]
        mono = stereo.mean(axis=1).astype(np.float32) if stereo.ndim == 2 else stereo.astype(np.float32)
        t = time.monotonic()
        interpreter.set_tensor(input_index, mono)
        interpreter.invoke()
        scores = interpreter.get_tensor(output_index)[0]
        assert np.isfinite(scores).all()
        top = np.argsort(scores)[-5:][::-1]
        power = (stereo**2).mean(axis=0) if stereo.ndim == 2 else np.array([1., 1.])
        imbalance = float(power.max()/max(1e-8,power.min()))
        singing = float(scores[24:33].max())
        records.append({'offsetMs': offset*1000//16000, 'speech': float(scores[0]), 'singing': singing,
                        'imbalance': imbalance, 'latencyMs': (time.monotonic()-t)*1000,
                        'candidate': bool(scores[0]>=.98 and singing<=.05 and imbalance>=4),
                        'top': [{'class':classes[i]['display_name'],'score':float(scores[i])} for i in top]})
    return records

rows = []
for sample in sorted((root/'app/src/androidTest/assets/cleanup-ab').glob('*.wav')):
    with wave.open(str(sample)) as audio:
        assert audio.getframerate()==16000 and audio.getnchannels()==2 and audio.getsampwidth()==2
        waveform = np.frombuffer(audio.readframes(audio.getnframes()), dtype='<i2').reshape(-1,2).astype(np.float32)/32768.
    rows.append({'sample':sample.name,'checksum':hashlib.sha256(sample.read_bytes()).hexdigest(),'windows':classify(waveform)})
result = {'modelSHA256':hashlib.sha256(model.read_bytes()).hexdigest(),'sampleCount':len(rows),
          'candidateWindows':sum(w['candidate'] for r in rows for w in r['windows']),
          'kind':'controlled mixtures; not real-club quality acceptance','samples':rows}
(root/'docs/verification/cleanup-model-host.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in result.items() if k!='samples'}))
