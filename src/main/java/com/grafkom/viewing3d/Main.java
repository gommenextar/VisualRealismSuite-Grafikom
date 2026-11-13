package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** 3D Visual Realism Suite - Praktikum Grafika Komputer Minggu 11. */
public class Main {
    private static final float GROUND_Y = -1.5f;
    private static final String[] OBJECT_NAMES = {"Sphere", "Torus"};

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    // State fitur interaktif
    private boolean blinnPhong = true;   // B
    private boolean smooth = true;       // F
    private boolean useTexture = true;   // T
    private boolean shadows = true;      // S
    private boolean animateLight = true; // SPACE
    private boolean spin = false;        // R
    private boolean vsync = true;        // V
    private int objectIndex = 0;         // M

    private final Camera camera = new Camera();
    private boolean dragging = false;
    private double lastX, lastY;

    private ShaderProgram phong, depth;
    private Texture2D texture;
    private ShadowMapFBO shadowFBO;
    private Mesh[] smoothMeshes, flatMeshes;
    private Mesh ground;

    private float lightAngle = 0.8f;
    private final Vector3f lightPos = new Vector3f();
    private final Vector3f lightColor = new Vector3f(1f, 1f, 1f);

    public static void main(String[] args) {
        new Main().run(args.length > 0 ? args[0] : "texture.png");
    }

    private void run(String texPath) {
        init(texPath);
        loop();
        cleanup();
    }

    private void init(String texPath) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(1280, 720, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window");
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> { fbWidth = w; fbHeight = h; });

        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE: glfwSetWindowShouldClose(win, true); break;
                case GLFW_KEY_B: blinnPhong = !blinnPhong; break;          // Indikator 1
                case GLFW_KEY_F: smooth = !smooth; break;                  // Indikator 2
                case GLFW_KEY_Z: HSRManager.toggleDepthTest(); break;      // Indikator 3
                case GLFW_KEY_C: HSRManager.toggleCulling(); break;        // Indikator 3
                case GLFW_KEY_T: useTexture = !useTexture; break;          // Indikator 4
                case GLFW_KEY_N: texture.toggleFilter(); break;            // Indikator 4
                case GLFW_KEY_S: shadows = !shadows; break;                // Indikator 5
                case GLFW_KEY_SPACE: animateLight = !animateLight; break;
                case GLFW_KEY_R: spin = !spin; break;
                case GLFW_KEY_M: objectIndex = (objectIndex + 1) % OBJECT_NAMES.length; break;
                case GLFW_KEY_V:
                    vsync = !vsync;
                    glfwSwapInterval(vsync ? 1 : 0);
                    break;
                default: break;
            }
        });
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) camera.rotate((float) (x - lastX), (float) (y - lastY));
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (win, dx, dy) -> camera.zoom((float) dy));

        // Resource
        phong = new ShaderProgram("phong.vert", "phong.frag");
        depth = new ShaderProgram("depth.vert", "depth.frag");
        texture = new Texture2D(texPath);
        shadowFBO = new ShadowMapFBO();

        MeshData[] shapes = {
                MeshData.sphere(1f, 48, 64),
                MeshData.torus(0.9f, 0.4f, 48, 24)
        };
        smoothMeshes = new Mesh[shapes.length];
        flatMeshes = new Mesh[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            smoothMeshes[i] = shapes[i].toSmoothMesh();
            flatMeshes[i] = shapes[i].toFlatMesh();
        }
        ground = MeshData.plane(7f, GROUND_Y, 4f).toFlatMesh();

        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(true);

        printHelp();
    }

    private void printHelp() {
        System.out.println("=== 3D Visual Realism Suite ===");
        System.out.println("Mouse kiri (drag) : putar kamera | Scroll : zoom");
        System.out.println("B : Phong <-> Blinn-Phong        F : Flat <-> Smooth");
        System.out.println("Z : Z-Buffer on/off              C : Back-Face Culling on/off");
        System.out.println("T : Tekstur on/off               N : Nearest <-> Bilinear+Mipmap");
        System.out.println("S : Shadow on/off                SPACE : animasi cahaya");
        System.out.println("R : putar objek                  M : ganti objek (Sphere/Torus)");
        System.out.println("V : VSync on/off (untuk ukur FPS) ESC : keluar");
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double fpsTimer = lastTime;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            if (animateLight) lightAngle += dt * 0.6f;
            lightPos.set(6f * (float) Math.cos(lightAngle), 7f, 6f * (float) Math.sin(lightAngle));

            render((float) now);

            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            if (now - fpsTimer >= 0.5) {
                updateTitle(frames / (now - fpsTimer));
                frames = 0;
                fpsTimer = now;
            }
        }
    }

    private void render(float time) {
        if (fbHeight <= 0 || fbWidth <= 0) return; // window diminimize

        // Matriks ruang cahaya (Directional Light -> Ortografik)
        Matrix4f lightProj = new Matrix4f().ortho(-10f, 10f, -10f, 10f, 1f, 25f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ---------- PASS 1: Depth Pass dari sudut pandang cahaya ----------
        if (shadows) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);            // selalu aktif di pass ini
            glDepthFunc(GL_LESS);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);               // render sisi belakang -> kurangi acne & peter panning
            depth.use();
            depth.setMat4("lightSpaceMatrix", lightSpace);
            drawObject(depth, time);            // hanya objek yang melempar bayangan
            glCullFace(GL_BACK);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ---------- PASS 2: Lighting Pass dari sudut pandang kamera ----------
        HSRManager.applyState();
        glClearColor(0.55f, 0.72f, 0.90f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(45.0), (float) fbWidth / fbHeight, 0.1f, 100f);

        phong.use();
        phong.setMat4("view", camera.getViewMatrix());
        phong.setMat4("projection", projection);
        phong.setMat4("lightSpaceMatrix", lightSpace);
        phong.setVec3("lightPos", lightPos);
        phong.setVec3("viewPos", camera.getPosition());
        phong.setVec3("lightColor", lightColor);
        phong.setBool("useTexture", useTexture);
        phong.setBool("useBlinnPhong", blinnPhong);
        phong.setBool("useShadow", shadows);
        phong.setInt("textureSampler", 0);
        phong.setInt("shadowMap", 1);
        texture.bind(0);
        shadowFBO.bindDepthTexture(1);

        // Lantai (penerima bayangan)
        phong.setMat4("model", new Matrix4f());
        phong.setVec3("objectColor", 0.45f, 0.55f, 0.45f);
        ground.draw();

        // Objek utama
        phong.setVec3("objectColor", 0.85f, 0.45f, 0.30f);
        drawObject(phong, time);
    }

    private void drawObject(ShaderProgram sh, float time) {
        Matrix4f model = new Matrix4f();
        if (spin) model.rotateY(time * 0.5f);
        sh.setMat4("model", model);
        Mesh m = smooth ? smoothMeshes[objectIndex] : flatMeshes[objectIndex];
        m.draw();
    }

    private void updateTitle(double fps) {
        glfwSetWindowTitle(window, String.format(
                "3D Visual Realism Suite | FPS %.0f | %s | %s | %s | Z-Buffer %s | Culling %s | Tex %s (%s) | Shadow %s",
                fps,
                OBJECT_NAMES[objectIndex],
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                HSRManager.isDepthEnabled() ? "ON" : "OFF",
                HSRManager.isCullEnabled() ? "ON" : "OFF",
                useTexture ? "ON" : "OFF",
                texture.isMipmapped() ? "Bilinear+Mipmap" : "Nearest",
                shadows ? "ON" : "OFF"));
    }

    private void cleanup() {
        for (Mesh m : smoothMeshes) m.cleanup();
        for (Mesh m : flatMeshes) m.cleanup();
        ground.cleanup();
        texture.cleanup();
        shadowFBO.cleanup();
        phong.cleanup();
        depth.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
