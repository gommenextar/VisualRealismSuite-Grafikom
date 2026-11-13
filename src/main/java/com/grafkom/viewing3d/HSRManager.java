package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class HSRManager {
    private static boolean depthEnabled = true;
    private static boolean cullEnabled = true;

    public static void setupDepthTest(boolean enable) {
        depthEnabled = enable;
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        cullEnabled = enable;
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);   // buang polygon belakang
            glFrontFace(GL_CCW);   // counter-clockwise = tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public static void toggleDepthTest()   { setupDepthTest(!depthEnabled); }
    public static void toggleCulling()     { setupBackFaceCulling(!cullEnabled); }
    public static boolean isDepthEnabled() { return depthEnabled; }
    public static boolean isCullEnabled()  { return cullEnabled; }

    /** Terapkan ulang state (dipanggil setelah depth pass mengubah state GL). */
    public static void applyState() {
        setupDepthTest(depthEnabled);
        setupBackFaceCulling(cullEnabled);
    }
}
