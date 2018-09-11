package com.danthought.livewallpaper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.provider.Settings;
import android.util.Log;

import com.danthought.livewallpaper.objects.Heightmap;
import com.danthought.livewallpaper.objects.ParticleShooter;
import com.danthought.livewallpaper.objects.ParticleSystem;
import com.danthought.livewallpaper.objects.Skybox;
import com.danthought.livewallpaper.programs.HeightmapShaderProgram;
import com.danthought.livewallpaper.programs.ParticleShaderProgram;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;
import static android.opengl.GLUtils.*;
import static android.opengl.Matrix.*;

import static com.danthought.livewallpaper.util.Geometry.*;

import com.danthought.livewallpaper.programs.SkyboxShaderProgram;
import com.danthought.livewallpaper.util.MatrixHelper;
import com.danthought.livewallpaper.util.TextureHelper;

/**
 * Created by danjiang on 2018/8/20.
 */

public class ParticlesRenderer implements GLSurfaceView.Renderer {
    private final Context context;

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrixForSkybox = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] it_modelViewMatrix = new float[16];

    private ParticleShaderProgram particleProgram;
    private ParticleSystem particleSystem;
    private ParticleShooter redParticleShooter;
    private ParticleShooter greenParticleShooter;
    private ParticleShooter blueParticleShooter;
    private long globalStartTime;
    private int particleTexture;

    private SkyboxShaderProgram skyboxProgram;
    private Skybox skybox;
    private int skyboxTexture;

    private float xRotation, yRotation;

    private HeightmapShaderProgram heightmapProgram;
    private Heightmap heightmap;

    private final float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};

    private final float[] pointLightPositions = new float[]
                    {-1f, 1f, 0f, 1f,
                    0f, 1f, 0f, 1f,
                    1f, 1f, 0f, 1f};

    private final float[] pointLightColors = new float[]
                    {1.00f, 0.20f, 0.02f,
                    0.02f, 0.25f, 0.02f,
                    0.02f, 0.20f, 1.00f};

    public ParticlesRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        particleProgram = new ParticleShaderProgram(context);
        particleSystem = new ParticleSystem(10000);
        globalStartTime = System.nanoTime();

        final Vector particleDirection = new Vector(0f, 0.5f, 0f);
        final float angleVarianceInDegrees = 5f;
        final float speedVariance = 1f;

        redParticleShooter = new ParticleShooter(
                new Point(-1f, 0f, 0f),
                particleDirection,
                Color.rgb(255, 50, 5),
                angleVarianceInDegrees,
                speedVariance);

        greenParticleShooter = new ParticleShooter(
                new Point(0f, 0f, 0f),
                particleDirection,
                Color.rgb(25, 255, 25),
                angleVarianceInDegrees,
                speedVariance);

        blueParticleShooter = new ParticleShooter(
                new Point(1f, 0f, 0f),
                particleDirection,
                Color.rgb(5, 50, 255),
                angleVarianceInDegrees,
                speedVariance);

        particleTexture = TextureHelper.loadTexture(context, R.drawable.particle_texture);

        skyboxProgram = new SkyboxShaderProgram(context);
        skybox = new Skybox();
        skyboxTexture = TextureHelper.loadCubeMap(context, new int[] {
                R.drawable.night_left, R.drawable.night_right,
                R.drawable.night_bottom, R.drawable.night_top,
                R.drawable.night_front, R.drawable.night_back
        });

        heightmapProgram = new HeightmapShaderProgram(context);
        heightmap = new Heightmap(((BitmapDrawable)context.getResources().getDrawable(R.drawable.heightmap)).getBitmap());
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        glViewport(0, 0, width, height);
        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 100f);
        updateViewMatrices();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        drawHeightmap();
        drawSkybox();
        drawParticles();
    }

    private void updateViewMatrices() {
        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);

        translateM(viewMatrix, 0, 0, -1.5f, -5f);
    }

    private void updateMvpMatrix() {
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        invertM(tempMatrix, 0, modelViewMatrix, 0);
        transposeM(it_modelViewMatrix, 0, tempMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    }

    private void updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }

    private void drawSkybox() {
        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();

        skyboxProgram.useProgram();
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        glDepthFunc(GL_LEQUAL);
        skybox.draw();
        glDepthFunc(GL_LESS);
    }

    private void drawParticles() {
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

        redParticleShooter.addParticles(particleSystem, currentTime, 1);
        greenParticleShooter.addParticles(particleSystem, currentTime, 1);
        blueParticleShooter.addParticles(particleSystem, currentTime, 1);

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);

        particleProgram.useProgram();
        particleProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture);
        particleSystem.bindData(particleProgram);
        glDepthMask(false);
        particleSystem.draw();
        glDepthMask(true);

        glDisable(GL_BLEND);
    }

    private void drawHeightmap() {
        setIdentityM(modelMatrix, 0);
        scaleM(modelMatrix, 0, 100f, 10f, 100f);
        updateMvpMatrix();
        heightmapProgram.useProgram();

        final float[] vectorToLightInEyeSpace = new float[4];
        final float[] pointPositionsInEyeSpace = new float[12];
        multiplyMV(vectorToLightInEyeSpace, 0, viewMatrix, 0, vectorToLight, 0);
        multiplyMV(pointPositionsInEyeSpace, 0, viewMatrix, 0, pointLightPositions, 0);
        multiplyMV(pointPositionsInEyeSpace, 4, viewMatrix, 0, pointLightPositions, 4);
        multiplyMV(pointPositionsInEyeSpace, 8, viewMatrix, 0, pointLightPositions, 8);
        heightmapProgram.setUniforms(modelViewMatrix, it_modelViewMatrix, modelViewProjectionMatrix, vectorToLightInEyeSpace, pointPositionsInEyeSpace, pointLightColors);
        heightmap.bindData(heightmapProgram);
        heightmap.draw();
    }

    public void handleTouchDrag(float deltaX, float deltaY) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;

        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        }

        updateViewMatrices();
    }
}
