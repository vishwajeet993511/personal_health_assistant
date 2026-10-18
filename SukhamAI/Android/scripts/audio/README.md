# Breathing audio source

`breathing_inhale_source.wav` is a mono, 24 kHz PCM conversion of the recording
supplied by the user, `freesound_community-longinhale-96120.mp3`. Its license
metadata was not included in the supplied file; no license is inferred here.

The original recording is approximately 5.9 seconds. The generator selects the
strongest continuous four seconds, preserves its pitch and playback speed,
adds 350 ms fades, and sets a gentle level. The hold is silent. Inhale and exhale use the same recording and base fades.
Exhale has an additional gradual four-second fade-out, ending smoothly in silence.

Run `python3 scripts/generate_breathing_audio.py` from the Android project to
rebuild the bundled twelve-second `breathing_cycle.wav` resource. The generator
uses only the Python standard library and the saved PCM source.
