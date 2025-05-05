/**
 * A simple 3D game using LWJGL (Lightweight Java Game Library) with OpenGL and GLFW.
 * Demonstrates camera movement with mouse and keyboard, rendering a colored cube,
 * and drawing a 2D background overlay.
 */
package org.example;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Simple3DGame {

    private long window;
    private float cameraX = 0, cameraY = 1.6f, cameraZ = 5;
    private float yaw = -90f, pitch = 0f;
    private float mouseSensitivity = 0.1f;
    private double lastMouseX = 400, lastMouseY = 300;
    private boolean firstMouse = true;

    /**
     * Entry point to start the application.
     */
    public void run() {
        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    /**
     * Initializes GLFW, OpenGL context, input callbacks, and perspective matrix.
     */
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "3D OpenGL Game", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);

        // Mouse movement callback
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            double xoffset = xpos - lastMouseX;
            double yoffset = lastMouseY - ypos;
            lastMouseX = xpos;
            lastMouseY = ypos;

            yaw += xoffset * mouseSensitivity;
            pitch += yoffset * mouseSensitivity;
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;
        });

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Set perspective projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float fov = 60f, aspect = 800f / 600f, zNear = 0.1f, zFar = 100f;
        float y_scale = (float)(1f / Math.tan(Math.toRadians(fov / 2f)));
        float x_scale = y_scale / aspect;

        FloatBuffer perspective = BufferUtils.createFloatBuffer(16);
        perspective.put(new float[]{
                x_scale, 0, 0, 0,
                0, y_scale, 0, 0,
                0, 0, -(zFar + zNear) / (zFar - zNear), -1,
                0, 0, -(2 * zFar * zNear) / (zFar - zNear), 0
        }).flip();
        glLoadMatrixf(perspective);
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Main rendering loop. Handles input, camera movement, and drawing.
     */
    private void loop() {
        float speed = 0.1f;

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glLoadIdentity();

            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }

            float radYaw = (float)Math.toRadians(yaw);
            float radPitch = (float)Math.toRadians(pitch);

            float dirX = (float)(Math.cos(radYaw) * Math.cos(radPitch));
            float dirY = (float)(Math.sin(radPitch));
            float dirZ = (float)(Math.sin(radYaw) * Math.cos(radPitch));

            float targetX = cameraX + dirX;
            float targetY = cameraY + dirY;
            float targetZ = cameraZ + dirZ;

            lookAt(cameraX, cameraY, cameraZ, targetX, targetY, targetZ, 0, 1, 0);

            float[] forward = { dirX, 0, dirZ };
            float[] right = { -dirZ, 0, dirX };

            float lenF = (float)Math.sqrt(forward[0]*forward[0] + forward[2]*forward[2]);
            float lenR = (float)Math.sqrt(right[0]*right[0] + right[2]*right[2]);
            forward[0] /= lenF; forward[2] /= lenF;
            right[0] /= lenR; right[2] /= lenR;

            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
                cameraX += forward[0] * speed;
                cameraZ += forward[2] * speed;
            }
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
                cameraX -= forward[0] * speed;
                cameraZ -= forward[2] * speed;
            }
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
                cameraX -= right[0] * speed;
                cameraZ -= right[2] * speed;
            }
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
                cameraX += right[0] * speed;
                cameraZ += right[2] * speed;
            }

            drawBackground();

            glPushMatrix();
            glTranslatef(0f, 1f, 0f);
            drawColoredCube();
            glPopMatrix();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /**
     * Simulates the camera view matrix.
     */
    private void lookAt(float eyeX, float eyeY, float eyeZ,
                        float centerX, float centerY, float centerZ,
                        float upX, float upY, float upZ) {
        float[] forward = {
                centerX - eyeX,
                centerY - eyeY,
                centerZ - eyeZ
        };
        float[] up = { upX, upY, upZ };

        float fLen = (float) Math.sqrt(forward[0]*forward[0] + forward[1]*forward[1] + forward[2]*forward[2]);
        forward[0] /= fLen;
        forward[1] /= fLen;
        forward[2] /= fLen;

        float[] side = {
                forward[1]*up[2] - forward[2]*up[1],
                forward[2]*up[0] - forward[0]*up[2],
                forward[0]*up[1] - forward[1]*up[0]
        };

        up[0] = side[1]*forward[2] - side[2]*forward[1];
        up[1] = side[2]*forward[0] - side[0]*forward[2];
        up[2] = side[0]*forward[1] - side[1]*forward[0];

        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        matrix.put(new float[]{
                side[0],   up[0],   -forward[0], 0,
                side[1],   up[1],   -forward[1], 0,
                side[2],   up[2],   -forward[2], 0,
                0,         0,        0,         1
        }).flip();

        glMultMatrixf(matrix);
        glTranslatef(-eyeX, -eyeY, -eyeZ);
    }

    /**
     * Draws a 2D background with sky and ground overlay.
     */
    private void drawBackground() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(-1, 1, -1, 1, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);

        glColor3f(0.5f, 0.8f, 1.0f);
        glBegin(GL_QUADS);
        glVertex2f(-1.0f, 1.0f);
        glVertex2f(1.0f, 1.0f);
        glVertex2f(1.0f, 0.0f);
        glVertex2f(-1.0f, 0.0f);
        glEnd();

        glColor3f(0.2f, 0.6f, 0.2f);
        glBegin(GL_QUADS);
        glVertex2f(-1.0f, 0.0f);
        glVertex2f(1.0f, 0.0f);
        glVertex2f(1.0f, -1.0f);
        glVertex2f(-1.0f, -1.0f);
        glEnd();

        glEnable(GL_DEPTH_TEST);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Draws a colored cube with each face a different color.
     */
    private void drawColoredCube() {
        glBegin(GL_QUADS);

        glColor3f(1.0f, 0.0f, 0.0f); // Front
        glVertex3f(-1, -1,  1);
        glVertex3f( 1, -1,  1);
        glVertex3f( 1,  1,  1);
        glVertex3f(-1,  1,  1);

        glColor3f(0.0f, 1.0f, 0.0f); // Back
        glVertex3f(-1, -1, -1);
        glVertex3f(-1,  1, -1);
        glVertex3f( 1,  1, -1);
        glVertex3f( 1, -1, -1);

        glColor3f(0.0f, 0.0f, 1.0f); // Top
        glVertex3f(-1,  1, -1);
        glVertex3f(-1,  1,  1);
        glVertex3f( 1,  1,  1);
        glVertex3f( 1,  1, -1);

        glColor3f(1.0f, 1.0f, 0.0f); // Bottom
        glVertex3f(-1, -1, -1);
        glVertex3f( 1, -1, -1);
        glVertex3f( 1, -1,  1);
        glVertex3f(-1, -1,  1);

        glColor3f(0.0f, 1.0f, 1.0f); // Right
        glVertex3f( 1, -1, -1);
        glVertex3f( 1,  1, -1);
        glVertex3f( 1,  1,  1);
        glVertex3f( 1, -1,  1);

        glColor3f(1.0f, 0.0f, 1.0f); // Left
        glVertex3f(-1, -1, -1);
        glVertex3f(-1, -1,  1);
        glVertex3f(-1,  1,  1);
        glVertex3f(-1,  1, -1);

        glEnd();
    }

    /**
     * Program entry point.
     */
    public static void main(String[] args) {
        new Simple3DGame().run();
    }
}
