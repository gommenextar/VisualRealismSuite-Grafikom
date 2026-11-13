package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Data geometri mentah (posisi, UV, indeks CCW) + pembangun Mesh Flat / Smooth. */
public class MeshData {
    private final float[] positions; // x,y,z
    private final float[] uvs;       // u,v
    private final int[] indices;     // segitiga CCW (sisi luar)

    public MeshData(float[] positions, float[] uvs, int[] indices) {
        this.positions = positions;
        this.uvs = uvs;
        this.indices = indices;
    }

    private Vector3f pos(int i) {
        return new Vector3f(positions[3 * i], positions[3 * i + 1], positions[3 * i + 2]);
    }

    private Vector3f faceNormalUnnormalized(int a, int b, int c) {
        Vector3f pa = pos(a);
        Vector3f e1 = pos(b).sub(pa);
        Vector3f e2 = pos(c).sub(pa);
        return e1.cross(e2); // panjang = 2x luas segitiga (area-weighted)
    }

    private static void put(float[] dst, int vi, Vector3f p, Vector3f n, float u, float v) {
        int o = vi * 8;
        dst[o] = p.x; dst[o + 1] = p.y; dst[o + 2] = p.z;
        dst[o + 3] = n.x; dst[o + 4] = n.y; dst[o + 5] = n.z;
        dst[o + 6] = u; dst[o + 7] = v;
    }

    /** SMOOTH: N_vertex = normalisasi(sum N_face) dari semua face yang berbagi posisi vertex. */
    public Mesh toSmoothMesh() {
        int n = positions.length / 3;
        String[] keys = new String[n];
        Map<String, Vector3f> acc = new HashMap<>();
        for (int i = 0; i < n; i++) {
            // Las vertex berdasarkan posisi (agar seam UV / kutub tidak merusak normal)
            keys[i] = Math.round(positions[3 * i] * 1e4f) + "," +
                      Math.round(positions[3 * i + 1] * 1e4f) + "," +
                      Math.round(positions[3 * i + 2] * 1e4f);
            acc.putIfAbsent(keys[i], new Vector3f());
        }
        for (int t = 0; t < indices.length; t += 3) {
            int a = indices[t], b = indices[t + 1], c = indices[t + 2];
            Vector3f fn = faceNormalUnnormalized(a, b, c);
            acc.get(keys[a]).add(fn);
            acc.get(keys[b]).add(fn);
            acc.get(keys[c]).add(fn);
        }
        float[] data = new float[n * 8];
        for (int i = 0; i < n; i++) {
            Vector3f nv = new Vector3f(acc.get(keys[i]));
            if (nv.length() > 1e-8f) nv.normalize(); else nv.set(0, 1, 0);
            put(data, i, pos(i), nv, uvs[2 * i], uvs[2 * i + 1]);
        }
        return new Mesh(data, indices.clone());
    }

    /** FLAT: vertex diduplikasi per face, setiap face memakai satu normal yang sama. */
    public Mesh toFlatMesh() {
        float[] data = new float[indices.length * 8];
        int count = 0;
        for (int t = 0; t < indices.length; t += 3) {
            int a = indices[t], b = indices[t + 1], c = indices[t + 2];
            Vector3f fn = faceNormalUnnormalized(a, b, c);
            if (fn.length() < 1e-10f) continue; // segitiga degenerate (kutub bola)
            fn.normalize();
            put(data, count++, pos(a), fn, uvs[2 * a], uvs[2 * a + 1]);
            put(data, count++, pos(b), fn, uvs[2 * b], uvs[2 * b + 1]);
            put(data, count++, pos(c), fn, uvs[2 * c], uvs[2 * c + 1]);
        }
        int[] idx = new int[count];
        for (int i = 0; i < count; i++) idx[i] = i;
        return new Mesh(Arrays.copyOf(data, count * 8), idx);
    }

    // ------------------------------------------------------------------ generator

    public static MeshData sphere(float r, int stacks, int slices) {
        int vc = (stacks + 1) * (slices + 1);
        float[] p = new float[vc * 3];
        float[] t = new float[vc * 2];
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * j / slices;
                int v = i * (slices + 1) + j;
                p[3 * v]     = (float) (r * Math.sin(phi) * Math.cos(theta));
                p[3 * v + 1] = (float) (r * Math.cos(phi));
                p[3 * v + 2] = (float) (r * Math.sin(phi) * Math.sin(theta));
                t[2 * v]     = (float) j / slices;
                t[2 * v + 1] = 1f - (float) i / stacks;
            }
        }
        return new MeshData(p, t, gridIndices(stacks, slices));
    }

    public static MeshData torus(float R, float r, int rings, int sides) {
        int vc = (rings + 1) * (sides + 1);
        float[] p = new float[vc * 3];
        float[] t = new float[vc * 2];
        for (int i = 0; i <= rings; i++) {
            double u = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double v = 2.0 * Math.PI * j / sides;
                int k = i * (sides + 1) + j;
                p[3 * k]     = (float) ((R + r * Math.cos(v)) * Math.cos(u));
                p[3 * k + 1] = (float) (r * Math.sin(v));
                p[3 * k + 2] = (float) ((R + r * Math.cos(v)) * Math.sin(u));
                t[2 * k]     = 2f * i / rings;
                t[2 * k + 1] = (float) j / sides;
            }
        }
        return new MeshData(p, t, gridIndices(rings, sides));
    }

    /** Bidang lantai (normal +Y), UV diulang sebanyak 'tiling' kali. */
    public static MeshData plane(float half, float y, float tiling) {
        float[] p = {
                -half, y, -half,
                -half, y,  half,
                 half, y,  half,
                 half, y, -half };
        float[] t = { 0, 0,  0, tiling,  tiling, tiling,  tiling, 0 };
        int[] idx = { 0, 1, 2,  0, 2, 3 };
        return new MeshData(p, t, idx);
    }

    /** Indeks grid (rows x cols) dengan winding CCW menghadap keluar. */
    private static int[] gridIndices(int rows, int cols) {
        int[] idx = new int[rows * cols * 6];
        int w = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int a = i * (cols + 1) + j;
                int b = a + cols + 1;
                idx[w++] = a;     idx[w++] = a + 1; idx[w++] = b;
                idx[w++] = a + 1; idx[w++] = b + 1; idx[w++] = b;
            }
        }
        return idx;
    }
}
