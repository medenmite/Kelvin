package com.example.engine

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import com.example.model.HeatingStrength
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Runs an offscreen EGL14 Pbuffer OpenGL ES shader loop on a dedicated background thread
 * when Background Service mode is active while the Activity is paused/locked.
 * If the device/OEM blocks background EGL, [start] reports failure via callback so the
 * coordinator seamlessly boosts CPU workers as a fallback.
 */
class OffscreenGpuRunner {

    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    @Volatile
    private var currentStrength: HeatingStrength = HeatingStrength.MEDIUM

    fun updateStrength(strength: HeatingStrength) {
        currentStrength = strength
    }

    fun start(
        strength: HeatingStrength,
        onFpsMeasured: (Int) -> Unit,
        onUnavailable: () -> Unit,
    ) {
        currentStrength = strength
        if (running.getAndSet(true)) return

        thread = Thread({
            var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
            var context: EGLContext = EGL14.EGL_NO_CONTEXT
            var surface: EGLSurface = EGL14.EGL_NO_SURFACE
            try {
                display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (display == EGL14.EGL_NO_DISPLAY) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }
                val version = IntArray(2)
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }

                val configAttribs = intArrayOf(
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE,
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
                    numConfigs[0] <= 0 ||
                    configs[0] == null
                ) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }

                val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
                if (context == EGL14.EGL_NO_CONTEXT) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }

                val pbufferSize = 512
                val surfaceAttribs = intArrayOf(
                    EGL14.EGL_WIDTH, pbufferSize,
                    EGL14.EGL_HEIGHT, pbufferSize,
                    EGL14.EGL_NONE,
                )
                surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
                if (surface == EGL14.EGL_NO_SURFACE || !EGL14.eglMakeCurrent(display, surface, surface, context)) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }

                val program = GpuStressRenderer.buildProgram(
                    """
                    attribute vec2 aPosition;
                    varying vec2 vUv;
                    void main() {
                        vUv = aPosition * 0.5 + 0.5;
                        gl_Position = vec4(aPosition, 0.0, 1.0);
                    }
                    """.trimIndent(),
                    GpuStressRenderer.STRESS_FRAGMENT_SHADER,
                )
                if (program == 0) {
                    running.set(false)
                    onUnavailable()
                    return@Thread
                }

                val quadBuffer: FloatBuffer = ByteBuffer
                    .allocateDirect(8 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .apply {
                        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
                        position(0)
                    }

                val startNs = System.nanoTime()
                var frames = 0
                var fpsWindowNs = startNs

                while (running.get()) {
                    val activeStrength = currentStrength
                    val elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000f
                    GLES20.glViewport(0, 0, pbufferSize, pbufferSize)
                    GLES20.glUseProgram(program)
                    val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
                    GLES20.glEnableVertexAttribArray(posLoc)
                    GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
                    GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTime"), elapsedSec)
                    GLES20.glUniform1i(
                        GLES20.glGetUniformLocation(program, "uIterations"),
                        activeStrength.shaderIterations,
                    )
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                    GLES20.glFinish()
                    GLES20.glDisableVertexAttribArray(posLoc)

                    frames++
                    val now = System.nanoTime()
                    val delta = now - fpsWindowNs
                    if (delta >= 500_000_000L) {
                        onFpsMeasured(((frames * 1_000_000_000.0) / delta).roundToInt().coerceAtLeast(1))
                        frames = 0
                        fpsWindowNs = now
                    }
                }
            } catch (_: Throwable) {
                if (running.getAndSet(false)) {
                    onUnavailable()
                }
            } finally {
                runCatching {
                    if (display != EGL14.EGL_NO_DISPLAY) {
                        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                        if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                        EGL14.eglTerminate(display)
                    }
                }
                onFpsMeasured(0)
            }
        }, "EmberCore-OffscreenGL").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        thread?.interrupt()
        thread = null
    }
}
