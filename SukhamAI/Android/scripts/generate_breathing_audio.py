"""Build recorded breathing cues: 4s inhale, 4s quiet hold, 4s fading exhale.

Inhale supplied by the user: freesound_community-longinhale-96120.mp3.
The source is decoded to mono 24kHz PCM in audio/breathing_inhale_source.wav.
Select the strongest four-second section without changing pitch or speed.
All phase edges fade to zero. Playback works offline.
"""
import array
import math
from pathlib import Path
import struct
import wave

rate = 24000
output = Path(__file__).resolve().parents[1] / 'src/app/src/main/res/raw/breathing_cycle.wav'
source = Path(__file__).resolve().parent / 'audio/breathing_inhale_source.wav'
with wave.open(str(source), 'rb') as recording:
    if (recording.getnchannels(), recording.getsampwidth(), recording.getframerate()) != (1, 2, rate):
        raise ValueError('Inhale source must be mono 16-bit PCM at 24000 Hz')
    inhale_samples = array.array('h', recording.readframes(recording.getnframes()))
if len(inhale_samples) < rate * 4:
    raise ValueError('Inhale recording must contain at least four seconds')
# Search at 100ms intervals for the most audible continuous breath.
window = rate * 4
hop = rate // 10
energy = [0]
for sample in inhale_samples:
    energy.append(energy[-1] + sample * sample)
starts = list(range(0, len(inhale_samples) - window + 1, hop))
start = max(starts, key=lambda index: energy[index + window] - energy[index])
inhale_samples = inhale_samples[start:start + window]
# Match the previous gentle pad's RMS, with a conservative hard peak ceiling.
rms = math.sqrt(sum(x * x for x in inhale_samples) / len(inhale_samples))
peak = max(map(abs, inhale_samples))
gain = min(0.032 * 32767 / max(rms, 1), 0.16 * 32767 / max(peak, 1))
print(f'Inhale source section: {start / rate:.1f}s to {start / rate + 4:.1f}s')
# Use the same recording and base fades for both phases.
cue = bytearray()
for i, sample in enumerate(inhale_samples):
    t = i / rate
    edge = min(t / 0.35, (4 - t) / 0.35, 1)
    fade = 0.5 - 0.5 * math.cos(math.pi * max(0, edge))
    cue.extend(struct.pack('<h', round(sample * gain * fade)))
# Exhale gradually softens throughout the four seconds, reaching silence smoothly.
exhale = bytearray()
for i, (sample,) in enumerate(struct.iter_unpack('<h', cue)):
    progress = i / (window - 1)
    fade_out = math.cos(math.pi * progress / 2) ** 2
    exhale.extend(struct.pack('<h', round(sample * fade_out)))
samples = cue + bytes(window * 2) + exhale
with wave.open(str(output), 'wb') as audio:
    audio.setnchannels(1)
    audio.setsampwidth(2)
    audio.setframerate(rate)
    audio.writeframes(samples)
print(output)
