package com.simibubi.create.foundation.ponder.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.PonderRegistry;

public class PonderIndex {
	
	public static final boolean EDITOR_MODE = true;

	public static void register() {
		// Register storyboards here (Changes require re-launch)

		PonderRegistry.forComponent(AllBlocks.SHAFT)
			.addStoryBoard("shaft/relay", KineticsScenes::shaftAsRelay)
			.addStoryBoard("shaft/encasing", KineticsScenes::shaftsCanBeEncased);

		// Debug scenes, can be found in game via the Brass Hand
		if (EDITOR_MODE)
			DebugScenes.registerAll();
	}

}
