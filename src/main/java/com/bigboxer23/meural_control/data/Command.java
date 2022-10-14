package com.bigboxer23.meural_control.data;

import java.io.IOException;

/**
 *
 */
@FunctionalInterface
public interface Command<T>
{
	T execute() throws IOException;
}
