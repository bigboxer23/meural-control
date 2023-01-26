package com.bigboxer23.meural_control;

import com.bigboxer23.meural_control.data.SourceItem;
import java.util.Optional;

/** Interface to define sources of content for the Meural */
public interface IMeuralImageSource {
	Optional<SourceItem> nextItem();

	Optional<SourceItem> prevItem();
}
