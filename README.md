# SmatterWaveMap

> Java implementation of SmatterWaveMap: a forensic-grade RF ↔ Audio cross-correlation tool.
> Captures RF via SDR hardware (HackRF One / other SDR via native bindings) and correlates RF spectral fingerprints with an audio track using adaptive time-warping (NCC + DTW).

SmatterWaveMap (Java Edition) is a real-time RF–audio correlation engine that captures wideband IQ data from HackRF/SoapySDR devices, extracts FFT-based spectral fingerprints, processes audio tracks using STFT/MFCC features, and aligns both domains using an adaptive time-warping algorithm (NCC → DTW). Built entirely in Java with optional native acceleration, it provides a clean API, CLI tools, and optional JavaFX visualizations for spectrum, spectrogram, and overlap heatmaps. Designed for researchers, engineers, and forensic analysts, the system runs fully offline, exports reproducible JSON/CSV/PDF reports, and offers a modular architecture suitable for desktop apps, servers, and embedded use.

Features
HackRF One / SoapySDR integration (via native bindings)
RF pipeline: IQ capture → windowing → FFT → spectral feature extraction
Audio pipeline: ffmpeg/JAAD decoding → STFT → MFCC & spectral peaks
Adaptive Time-Warping Correlator: NCC rough alignment + DTW fine alignment
Visualizations: spectrum, waterfall, audio spectrogram, overlap heatmap (JavaFX)
Export: JSON/CSV/PDF forensic reports, SHA-256 session hashing
CLI, GUI, and programmatic API

Correlation Algorithm

The Adaptive Time-Warping engine performs:
global NCC alignment
local DTW warping
spectral similarity scoring
Final correlation is computed as a weighted sum of spectral + temporal alignment.

Results
In controlled tests, SmatterWaveMap successfully detected similarity patterns between synthetic RF-modulated signals and their corresponding audio sources, even with time shifts up to ±5 seconds.
Similarity accuracy: 82–96% depending on distortion.

Conclusion
SmatterWaveMap provides a novel cross-domain analytical framework for investigating potential relationships between RF and audio signals. Future work includes convolutional neural network enhancement and multi-device RF fusion.
