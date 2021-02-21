package com.simibubi.create.foundation.ponder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.foundation.ponder.elements.PonderOverlayElement;
import com.simibubi.create.foundation.ponder.elements.PonderSceneElement;
import com.simibubi.create.foundation.ponder.elements.WorldSectionElement;
import com.simibubi.create.foundation.ponder.instructions.HideAllInstruction;
import com.simibubi.create.foundation.renderState.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.LerpedFloat;
import com.simibubi.create.foundation.utility.MatrixStacker;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.outliner.Outliner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PonderScene {

	boolean finished;
	int sceneIndex;
	
	List<PonderInstruction> schedule, activeSchedule;
	Map<UUID, PonderElement> linkedElements;
	Set<PonderElement> elements;
	
	PonderWorld world;
	ResourceLocation component;
	SceneTransform transform;
	SceneRenderInfo info;
	Outliner outliner;
	String defaultTitle;

	Vec3d pointOfInterest;
	Vec3d chasingPointOfInterest;
	WorldSectionElement baseWorldSection;

	int offsetX;
	int offsetZ;
	int size;

	public PonderScene(PonderWorld world, ResourceLocation component, int sceneIndex) {
		pointOfInterest = Vec3d.ZERO;

		this.world = world;
		this.component = component;
		this.sceneIndex = sceneIndex;

		outliner = new Outliner();
		elements = new HashSet<>();
		linkedElements = new HashMap<>();
		schedule = new ArrayList<>();
		activeSchedule = new ArrayList<>();
		transform = new SceneTransform();
		size = getBounds().getXSize();
		info = new SceneRenderInfo();
		baseWorldSection = new WorldSectionElement();

		PonderLocalization.registerSpecific(component, sceneIndex, "title", "Untitled Scene");
		setPointOfInterest(new Vec3d(0, 4, 0));
	}

	public String getTitle() {
		return getString("title");
	}

	public String getString(String key) {
		return PonderLocalization.getSpecific(component, sceneIndex, key);
	}

	public void reset() {
		activeSchedule.clear();
		schedule.forEach(mdi -> mdi.reset(this));
	}

	public void begin() {
		reset();
		world.restore();
		transform = new SceneTransform();
		finished = false;
		setPointOfInterest(new Vec3d(0, 4, 0));
		forEach(pe -> pe.reset(this));
		elements.clear();
		linkedElements.clear();
		activeSchedule.addAll(schedule);
		
		baseWorldSection.setEmpty();
		baseWorldSection.forceApplyFade(1);
		elements.add(baseWorldSection);
	}

	public WorldSectionElement getBaseWorldSection() {
		return baseWorldSection;
	}

	public void fadeOut() {
		reset();
		activeSchedule.add(new HideAllInstruction(10, null));
	}

	public void renderScene(SuperRenderTypeBuffer buffer, MatrixStack ms) {
		float pt = Minecraft.getInstance()
			.getRenderPartialTicks();

		ms.push();
		forEachVisible(PonderSceneElement.class, e -> e.renderFirst(world, buffer, ms));
		for (RenderType type : RenderType.getBlockLayers())
			forEachVisible(PonderSceneElement.class, e -> e.renderLayer(world, buffer, type, ms));
		forEachVisible(PonderSceneElement.class, e -> e.renderLast(world, buffer, ms));
		info.set(transform.xRotation.getValue(pt), transform.yRotation.getValue(pt));
		world.renderParticles(ms, buffer, info);
		outliner.renderOutlines(ms, buffer);
		ms.pop();
	}

	public void renderOverlay(PonderUI screen, MatrixStack ms, float partialTicks) {
		ms.push();
		forEachVisible(PonderOverlayElement.class, e -> e.render(this, screen, ms, partialTicks));
		ms.pop();
	}

	public void setPointOfInterest(Vec3d poi) {
		if (chasingPointOfInterest == null)
			pointOfInterest = poi;
		chasingPointOfInterest = poi;
	}

	public Vec3d getPointOfInterest() {
		return pointOfInterest;
	}

	public void tick() {
		if (chasingPointOfInterest != null)
			pointOfInterest = VecHelper.lerp(.25f, pointOfInterest, chasingPointOfInterest);

		outliner.tickOutlines();
		world.tickParticles();
		transform.tick();
		forEach(e -> e.tick(this));

		for (Iterator<PonderInstruction> iterator = activeSchedule.iterator(); iterator.hasNext();) {
			PonderInstruction instruction = iterator.next();
			instruction.tick(this);
			if (instruction.isComplete()) {
				iterator.remove();
				continue;
			}
			if (instruction.isBlocking())
				break;
		}

		if (activeSchedule.isEmpty())
			finished = true;
	}

	public void addElement(PonderElement e) {
		elements.add(e);
	}

	public <E extends PonderElement> void linkElement(E e, ElementLink<E> link) {
		linkedElements.put(link.getId(), e);
	}

	public <E extends PonderElement> E resolve(ElementLink<E> link) {
		return link.cast(linkedElements.get(link.getId()));
	}

	public <E extends PonderElement> void runWith(ElementLink<E> link, Consumer<E> callback) {
		callback.accept(resolve(link));
	}

	public <E extends PonderElement, F> F applyTo(ElementLink<E> link, Function<E, F> function) {
		return function.apply(resolve(link));
	}

	public PonderWorld getWorld() {
		return world;
	}

	public Set<PonderElement> getElements() {
		return elements;
	}

	public void forEach(Consumer<? super PonderElement> function) {
		for (PonderElement elemtent : elements)
			function.accept(elemtent);
	}

	public <T extends PonderElement> void forEach(Class<T> type, Consumer<T> function) {
		for (PonderElement element : elements)
			if (type.isInstance(element))
				function.accept(type.cast(element));
	}

	public <T extends PonderElement> void forEachVisible(Class<T> type, Consumer<T> function) {
		for (PonderElement element : elements)
			if (type.isInstance(element) && element.isVisible())
				function.accept(type.cast(element));
	}

	public MutableBoundingBox getBounds() {
		return world == null ? new MutableBoundingBox() : world.getBounds();
	}

	public SceneBuilder builder() {
		return new SceneBuilder(this);
	}

	public SceneBuildingUtil getSceneBuildingUtil() {
		return new SceneBuildingUtil(getBounds());
	}

	Supplier<String> textGetter(String key) {
		return () -> PonderLocalization.getSpecific(component, sceneIndex, key);
	}

	public SceneTransform getTransform() {
		return transform;
	}

	public class SceneTransform {

		public LerpedFloat xRotation, yRotation;

		// Screen params
		int width, height;
		double offset;
		Matrix4f cachedMat;

		public SceneTransform() {
			xRotation = LerpedFloat.angular()
				.startWithValue(-35);
			yRotation = LerpedFloat.angular()
				.startWithValue(55 + 90);
		}

		public void tick() {
			xRotation.tickChaser();
			yRotation.tickChaser();
		}

		public void updateScreenParams(int width, int height, double offset) {
			this.width = width;
			this.height = height;
			this.offset = offset;
			cachedMat = null;
		}

		public MatrixStack apply(MatrixStack ms) {
			float pt = Minecraft.getInstance()
				.getRenderPartialTicks();
			ms.translate(width / 2, height / 2, 200);

			MatrixStacker.of(ms)
				.rotateX(-35)
				.rotateY(55);
			ms.translate(offset, 0, 0);
			MatrixStacker.of(ms)
				.rotateY(-55)
				.rotateX(35);

			MatrixStacker.of(ms)
				.rotateX(xRotation.getValue(pt))
				.rotateY(yRotation.getValue(pt));
			ms.scale(30, -30, 30);
			ms.translate((size + offsetX) / -2f, -.5f, (size + offsetZ) / -2f);

			return ms;
		}

		public Vec3d screenToScene(float x, float y) {
			refreshMatrix();
			Vector4f vec = new Vector4f(x, y, 0, 1);
			cachedMat.invert();
			vec.transform(cachedMat);
			cachedMat.invert();
			MutableBoundingBox bounds = getBounds();
			return new Vec3d(vec.getX() + bounds.getXSize() / -2f, vec.getY(), vec.getZ() + bounds.getZSize() / -2f);
		}

		public Vec2f sceneToScreen(Vec3d vec) {
			refreshMatrix();
			Vector4f vec4 = new Vector4f((float) vec.x, (float) vec.y, (float) vec.z, 1);
			vec4.transform(cachedMat);
			return new Vec2f(vec4.getX(), vec4.getY());
		}

		protected void refreshMatrix() {
			if (cachedMat != null)
				return;
			cachedMat = apply(new MatrixStack()).peek()
				.getModel();
		}

	}

	public class SceneRenderInfo extends ActiveRenderInfo {

		public void set(float xRotation, float yRotation) {
			setDirection(yRotation, xRotation);
		}

	}

	public Outliner getOutliner() {
		return outliner;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

}