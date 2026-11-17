package com.example.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.HeatingStrength
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

class GpuStressRenderer(
    private val onFpsMeasured: (Int) -> Unit,
) : GLSurfaceView.Renderer {

    @Volatile
    var strength: HeatingStrength = HeatingStrength.MEDIUM

    private var stressProgram = 0
    private var displayProgram = 0
    private var fboId = 0
    private var fboTextureId = 0
    private var currentFboSize = 0
    private var viewWidth = 1
    private var viewHeight = 1
    private var startTimeNs = System.nanoTime()
    private var frameCount = 0
    private var lastFpsTimestampNs = System.nanoTime()

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.925f, 0.933f, 0.953f, 1f)
        stressProgram = buildProgram(VERTEX_SHADER, STRESS_FRAGMENT_SHADER)
        displayProgram = buildProgram(VERTEX_SHADER, DISPLAY_FRAGMENT_SHADER)
        rebuildFboIfNeeded(strength.fboSize)
        startTimeNs = System.nanoTime()
        lastFpsTimestampNs = startTimeNs
        frameCount = 0
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
    }

    override fun onDrawFrame(gl: GL10?) {
        val activeStrength = strength
        rebuildFboIfNeeded(activeStrength.fboSize)

        val elapsedSec = ((System.nanoTime() - startTimeNs) / 1_000_000_000f)

        // Pass 1: Heavy offscreen fragment shader ALU load across FBO texture
        if (fboId != 0 && stressProgram != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            GLES20.glViewport(0, 0, currentFboSize, currentFboSize)
            GLES20.glUseProgram(stressProgram)

            val posLoc = GLES20.glGetAttribLocation(stressProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)

            GLES20.glUniform1f(GLES20.glGetUniformLocation(stressProgram, "uTime"), elapsedSec)
            GLES20.glUniform1i(
                GLES20.glGetUniformLocation(stressProgram, "uIterations"),
                activeStrength.shaderIterations,
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(posLoc)
        }

        // Pass 2: Flat crisp geometric tile preview on screen (no gradients)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        if (displayProgram != 0) {
            GLES20.glUseProgram(displayProgram)
            val posLoc = GLES20.glGetAttribLocation(displayProgram, "aPosition")
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(displayProgram, "uTime"), elapsedSec)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(displayProgram, "uNoiseTex"), 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(posLoc)
        }

        frameCount++
        val now = System.nanoTime()
        val deltaNs = now - lastFpsTimestampNs
        if (deltaNs >= 500_000_000L) {
            val fps = ((frameCount * 1_000_000_000.0) / deltaNs).roundToInt().coerceAtLeast(1)
            onFpsMeasured(fps)
            frameCount = 0
            lastFpsTimestampNs = now
        }
    }

    private fun rebuildFboIfNeeded(targetSize: Int) {
        if (currentFboSize == targetSize && fboId != 0) return

        if (fboTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
            fboTextureId = 0
        }
        if (fboId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            fboId = 0
        }

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        fboTextureId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            targetSize,
            targetSize,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null,
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        val fboIds = IntArray(1)
        GLES20.glGenFramebuffers(1, fboIds, 0)
        fboId = fboIds[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0,
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        currentFboSize = targetSize
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            varying vec2 vUv;
            void main() {
                vUv = aPosition * 0.5 + 0.5;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        // Heavy ALU trigonometric / vector loop to exercise mobile GPU shader cores
        const val STRESS_FRAGMENT_SHADER = """
            precision highp float;
            varying vec2 vUv;
            uniform float uTime;
            uniform int uIterations;
            void main() {
                vec2 p = vUv * 3.14159 + vec2(uTime * 0.13, uTime * 0.09);
                vec4 acc = vec4(0.17, 0.31, 0.53, 1.0);
                for (int i = 0; i < 200; i++) {
                    if (i >= uIterations) break;
                    float fi = float(i) * 0.037;
                    p = vec2(
                        sin(p.x * 1.414 + p.y + fi) + cos(p.y * 1.732 - fi),
                        cos(p.y * 1.618 - p.x + fi) - sin(p.x * 1.272 + fi)
                    );
                    acc.rgb += vec3(
                        sin(p.x * 2.1 + acc.g),
                        cos(p.y * 2.3 + acc.b),
                        sin((p.x + p.y) * 1.9 + acc.r)
                    ) * 0.025;
                }
                gl_FragColor = vec4(fract(abs(acc.rgb)), 1.0);
            }
        """

        // Crisp flat geometric grid display with zero gradients
        private const val DISPLAY_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vUv;
            uniform float uTime;
            uniform sampler2D uNoiseTex;
            void main() {
                vec4 sampleVal = texture2D(uNoiseTex, vUv);
                vec2 grid = floor(vUv * 8.0) / 8.0;
                float cellPhase = fract(sin(dot(grid, vec2(12.9898, 78.233))) * 43758.5453 + uTime * 0.85);
                float activeCell = step(0.58, cellPhase + sampleVal.r * 0.12);
                vec3 bg = vec3(0.882, 0.898, 0.933);
                vec3 crimson = vec3(0.882, 0.114, 0.282);
                vec3 color = mix(bg, crimson, activeCell);
                gl_FragColor = vec4(color, 1.0);
            }
        """

        fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
            if (vs == 0 || fs == 0) return 0
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vs)
            GLES20.glAttachShader(program, fs)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(program)
                return 0
            }
            return program
        }

        private fun compileShader(type: Int, src: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, src)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }
    }
}

@Composable
fun GpuStressSurface(
    active: Boolean,
    strength: HeatingStrength,
    onFpsMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { GpuStressRenderer(onFpsMeasured) }
    renderer.strength = strength

    val glSurfaceView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView, active) {
        if (active) {
            glSurfaceView.onResume()
        } else {
            glSurfaceView.onPause()
            onFpsMeasured(0)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (active) glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    AndroidView(
        factory = { glSurfaceView },
        modifier = modifier,
    )
}
