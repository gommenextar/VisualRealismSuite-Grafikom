package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public class Texture2D {
    private final int textureID;
    private boolean mipmapped = true;

    public Texture2D(String filePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        stbi_set_flip_vertically_on_load(true); // v=0 di bawah, sesuai konvensi OpenGL
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer ch = stack.mallocInt(1);
            ByteBuffer data = stbi_load(filePath, w, h, ch, 4);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
                stbi_image_free(data);
                System.out.println("Tekstur dimuat: " + filePath);
            } else {
                System.err.println("Tekstur '" + filePath + "' tidak ditemukan, memakai checkerboard.");
                createChecker();
            }
        }
        glGenerateMipmap(GL_TEXTURE_2D);
        applyFilter();
    }

    private void createChecker() {
        int size = 256, cell = 32;
        ByteBuffer buf = memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean light = ((x / cell) + (y / cell)) % 2 == 0;
                buf.put((byte) (light ? 235 : 70));
                buf.put((byte) (light ? 225 : 90));
                buf.put((byte) (light ? 200 : 150));
                buf.put((byte) 255);
            }
        }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        memFree(buf);
    }

    private void applyFilter() {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void toggleFilter()  { mipmapped = !mipmapped; applyFilter(); }
    public boolean isMipmapped() { return mipmapped; }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() { glDeleteTextures(textureID); }
}
