package com.bigboxer23.meural_control;

import java.net.URL;
import java.util.Optional;

/**
 * Interface to define sources of content for the Meural
 */
public interface IMeuralImageSource
{
	Optional<URL> nextItem();
}
