package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertName, String fragName) {
        int vs = compile(GL_VERTEX_SHADER, load(vertName), vertName);
        int fs = compile(GL_FRAGMENT_SHADER, load(fragName), fragName);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String load(String name) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream("/shaders/" + name)) {
            if (in == null) throw new RuntimeException("Shader tidak ditemukan: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int compile(int type, String src, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Compile error di " + name + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String n, int v)         { glUniform1i(loc(n), v); }
    public void setBool(String n, boolean v)    { glUniform1i(loc(n), v ? 1 : 0); }
    public void setVec3(String n, Vector3f v)   { glUniform3f(loc(n), v.x, v.y, v.z); }
    public void setVec3(String n, float x, float y, float z) { glUniform3f(loc(n), x, y, z); }

    public void setMat4(String n, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(loc(n), false, fb);
        }
    }

    public void cleanup() { glDeleteProgram(programId); }
}
