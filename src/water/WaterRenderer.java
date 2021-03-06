package water;

import java.util.List;


import lights.PointLight;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;


import loaders.Loader;
import entities.Camera;
import renderEngine.MasterRenderer;
import renderEngine.RawModel;
import textures.Texture;
import toolBox.Maths;
import toolBox.Timer;

public class WaterRenderer {

	private static final String DUDV_MAP = "/textures/waterDUDV.png";
	private static final String NORMAL_MAP = "/textures/normalMap.png";


	private static final float WAVE_SPEED = 0.03f;

	private RawModel quad;
	private WaterShader shader;
	private WaterFrameBuffers fbos;

	private float moveFactor = 0;


	private int dudvMapTextureID;
	private int normalMapID;

	public WaterRenderer(Loader loader, WaterShader shader, Matrix4f projectionMatrix, WaterFrameBuffers fbos) {
		this.shader = shader;
		this.fbos = fbos;
		try {
			dudvMapTextureID = new Texture(DUDV_MAP, true).getId();
		} catch (Exception e) {
			System.out.println("Failed to load " + DUDV_MAP);
			e.printStackTrace();
		}

		try {
			normalMapID = new Texture(NORMAL_MAP, true).getId();
		} catch (Exception e) {
			System.out.println("Failed to load " + NORMAL_MAP);
			e.printStackTrace();
		}
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
		setUpVAO(loader);
	}

	public void render(List<WaterTile> water, Camera camera, PointLight pointLight) {
		prepareRender(camera, pointLight);
		for (WaterTile tile : water) {
			Matrix4f modelMatrix = Maths.createTransformationMatrix(
					new Vector3f(tile.getX(), tile.getHeight(), tile.getZ()), 0, 0, 0,
					WaterTile.TILE_SIZE);
			shader.loadModelMatrix(modelMatrix);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, quad.getVertexCount());
		}
		unbind();
	}
	
	private void prepareRender(Camera camera, PointLight pointLight){
		shader.start();
		shader.connectTextureUnits();
		shader.loadViewMatrix(camera);
		moveFactor += WAVE_SPEED * Timer.getDelta();
		moveFactor %= 1;
		shader.loadMoveFactor(moveFactor);
		shader.loadLight(pointLight);
		shader.loadNearFarPlane(MasterRenderer.NEAR_PLANE, MasterRenderer.FAR_PLANE);
		GL30.glBindVertexArray(quad.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getReflectionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, dudvMapTextureID);
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMapID);
		GL13.glActiveTexture(GL13.GL_TEXTURE4);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionDepthTexture());

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	private void unbind(){
		GL11.glDisable(GL11.GL_BLEND);

		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
		shader.stop();
	}

	private void setUpVAO(Loader loader) {
		// Just x and z vectex positions here, y is set to 0 in v.shader
		float[] vertices = { -1, -1, -1, 1, 1, -1, 1, -1, -1, 1, 1, 1 };
		quad = loader.loadToVAO(vertices, 2);
	}

}
