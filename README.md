# 3D Visual Realism Suite (Grafika Komputer - Minggu 11)

Java 17 + LWJGL 3 + JOML + GLSL 330 core.

## Menjalankan
    mvn compile exec:java
    mvn compile exec:java -Dexec.args="gambar.jpg"   # tekstur sendiri (default: texture.png, fallback checkerboard)

macOS: jalankan dari IDE dengan VM option -XstartOnFirstThread.

## Kontrol
| Tombol | Fungsi |
|---|---|
| Mouse kiri (drag) / scroll | Putar kamera / zoom |
| B | Phong <-> Blinn-Phong (Indikator 1) |
| F | Flat <-> Smooth shading (Indikator 2) |
| Z / C | Z-Buffer / Back-Face Culling on-off (Indikator 3) |
| T / N | Tekstur on-off / Nearest <-> Bilinear+Mipmap (Indikator 4) |
| S | Shadow Mapping on-off (Indikator 5) |
| V | VSync on-off (untuk ukur FPS) |
| SPACE / R / M | Animasi cahaya / putar objek / ganti Sphere-Torus |
| ESC | Keluar |

Status fitur dan FPS ditampilkan di judul window.

## Tuning bayangan
Bias ada di phong.frag (fungsi calcShadow): max(0.005 * (1 - N.L), 0.0005).
Naikkan jika ada Shadow Acne, turunkan jika terjadi Peter Panning.
