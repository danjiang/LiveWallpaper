package com.danthought.livewallpaper.objects;

import com.danthought.livewallpaper.data.VertexArray;
import com.danthought.livewallpaper.programs.SkyboxShaderProgram;

import java.nio.ByteBuffer;

import static android.opengl.GLES20.*;
import static android.opengl.GLUtils.*;
import static android.opengl.Matrix.*;

/**
 * Created by danjiang on 2018/8/21.
 */

public class Skybox {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private final VertexArray vertexArray;
    private final ByteBuffer indexArray;

    public Skybox() {
        vertexArray = new VertexArray(new float[] {
                -1, 1, 1,
                1, 1, 1,
                -1, -1, 1,
                1, -1, 1,
                -1, 1, -1,
                1, 1, -1,
                -1, -1, -1,
                1, -1, -1
        });

        indexArray = ByteBuffer.allocateDirect(6 * 6)
                .put(new byte[] {
                        // Front
                        1, 3, 0,
                        0, 3, 2,

                        // Back
                        4, 6, 5,
                        5, 6, 7,

                        // Left
                        0, 2, 4,
                        4, 2, 6,

                        // Right
                        5, 7, 1,
                        1, 7, 3,

                        // Top
                        5, 1, 4,
                        4, 1, 0,

                        // Bottom
                        6, 2, 7,
                        7, 2, 3
                });
        indexArray.position(0);
    }

    public void bindData(SkyboxShaderProgram skyboxProgram) {
        vertexArray.setVertexAttribPointer(0, skyboxProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_BYTE, indexArray);
    }

}
