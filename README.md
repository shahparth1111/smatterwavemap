# SmatterWaveMap
### *Real-Time RF ⇆ Audio Correlation Engine*

<p align="center">
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/License-CC0%201.0-lightgrey?style=for-the-badge">
  <img src="https://img.shields.io/badge/SDR-HackRF%20%7C%20SoapySDR-purple?style=for-the-badge">
</p>

SmatterWaveMap (Java Edition) is a **real-time RF–audio forensic analysis engine** that captures live IQ data from HackRF/SoapySDR devices, extracts spectral fingerprints, analyzes audio tracks with STFT/MFCC features, and correlates both domains using an **Adaptive Time-Warping algorithm**.
The project is built for **researchers, engineers, and signal-processing experts**, offering a smooth Java API, a CLI, and optional JavaFX visualizations.

## Features

* **Live RF Capture** — HackRF One & SoapySDR support
* **Audio Analysis** — STFT, MFCC, spectral peak extraction
* **Adaptive Correlation** — NCC + DTW hybrid algorithm
* **Forensic Exports** — JSON, CSV, PDF reports
* **Visualization** — Spectrum, spectrogram, heatmaps (JavaFX)
* **Offline & Secure** — No cloud, reproducible hashed logs

---

## 🗂 Project Structure

```
/smatterwavemap-java
├── core/        # FFT, filtering, feature extraction
├── audio/       # decoding + STFT/MFCC engine
├── device/      # HackRF/SoapySDR integration
├── gui/         # JavaFX visual views (optional)
```

---

## 📦 Installation

### **Clone + Build (Maven)**

```bash
git clone https://github.com/shahparth1111/smatterwavemap.git
cd smatterwavemap-java
mvn clean package
```

---

## ▶️ Quickstart (API Example)

```java
RFDevice dev = RFDeviceFactory.open("hackrf", 2_000_000, 20);
Correlator corr = new Correlator();

corr.loadAudio("track.wav");
CorrelationResult r = corr.streamAndCorrelate(dev, Duration.ofSeconds(10));

System.out.println("Similarity: " + r.getSimilarityPercent());
dev.close();
```

---

## 🖥 Run (CLI)

**Capture RF → Analyze against audio**

```bash
java -jar smatterwavemap.jar capture --duration 10s --out cap.iq
java -jar smatterwavemap.jar analyze --rf cap.iq --audio file.wav --out report.json
```

---

## 🎨 Visual Mode (JavaFX)

```bash
java -jar smatterwavemap.jar --gui
```

Displays:

* Live RF Spectrum
* Audio Spectrogram
* Overlap Heatmap
* Correlation Timeline
