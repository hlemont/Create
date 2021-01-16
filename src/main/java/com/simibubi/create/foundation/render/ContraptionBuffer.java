package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.simibubi.create.foundation.render.instancing.InstanceBuffer;
import com.simibubi.create.foundation.render.instancing.VertexFormat;
import net.minecraft.client.renderer.BufferBuilder;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

import static com.simibubi.create.foundation.render.instancing.VertexAttribute.*;

public class ContraptionBuffer extends TemplateBuffer {
    public static final VertexFormat FORMAT = new VertexFormat(InstanceBuffer.FORMAT, RGBA);

    protected int vao, ebo, vbo;

    public ContraptionBuffer(BufferBuilder buf) {
        super(buf);
        if (vertexCount > 0) setup();
    }

    public void delete() {
        if (vertexCount > 0) {
            RenderWork.enqueue(() -> {
                GL15.glDeleteBuffers(vbo);
                GL15.glDeleteBuffers(ebo);
                GL30.glDeleteVertexArrays(vao);
            });
        }
    }

    public void render() {
        if (vertexCount == 0) return;
        GL30.glBindVertexArray(vao);

        for (int i = 0; i <= 3; i++) {
            GL40.glEnableVertexAttribArray(i);
        }

        GlStateManager.bindBuffers(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);

        GL40.glDrawElements(GL11.GL_QUADS, vertexCount, GL11.GL_UNSIGNED_SHORT, 0);

        for (int i = 0; i <= FORMAT.getNumAttributes(); i++) {
            GL40.glDisableVertexAttribArray(i);
        }

        GlStateManager.bindBuffers(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void setup() {
        int stride = FORMAT.getStride();
        int invariantSize = vertexCount * stride;

        vao = GL30.glGenVertexArrays();
        ebo = GlStateManager.genBuffers();
        vbo = GlStateManager.genBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // allocate the buffer on the gpu
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, invariantSize, GL15.GL_STATIC_DRAW);

        // mirror it in system memory so we can write to it
        ByteBuffer constant = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER, GL15.GL_WRITE_ONLY);

        for (int i = 0; i < vertexCount; i++) {
            constant.putFloat(getX(template, i));
            constant.putFloat(getY(template, i));
            constant.putFloat(getZ(template, i));

            constant.put(getNX(template, i));
            constant.put(getNY(template, i));
            constant.put(getNZ(template, i));

            constant.putFloat(getU(template, i));
            constant.putFloat(getV(template, i));

            constant.put(getR(template, i));
            constant.put(getG(template, i));
            constant.put(getB(template, i));
            constant.put(getA(template, i));
        }
        constant.rewind();
        GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);

        buildEBO(ebo);

        FORMAT.informAttributes(0);

        GlStateManager.bindBuffers(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager.bindBuffers(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        // Deselect (bind to 0) the VAO
        GL30.glBindVertexArray(0);
    }
}
