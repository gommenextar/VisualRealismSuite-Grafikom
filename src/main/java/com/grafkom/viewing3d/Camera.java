package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit: drag mouse kiri untuk memutar, scroll untuk zoom. */
public class Camera {
    private float yaw = 0.7f, pitch = 0.5f, distance = 9f;
    private final Vector3f target = new Vector3f(0, -0.3f, 0);

    public void rotate(float dx, float dy) {
        yaw -= dx * 0.01f;
        pitch += dy * 0.01f;
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));
    }

    public void zoom(float scroll) {
        distance = Math.max(2f, Math.min(30f, distance - scroll * 0.5f));
    }

    public Vector3f getPosition() {
        return new Vector3f(
                target.x + distance * (float) (Math.cos(pitch) * Math.sin(yaw)),
                target.y + distance * (float) Math.sin(pitch),
                target.z + distance * (float) (Math.cos(pitch) * Math.cos(yaw)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0, 1, 0));
    }
}
