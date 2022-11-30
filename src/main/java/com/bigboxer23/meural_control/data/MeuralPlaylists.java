package com.bigboxer23.meural_control.data;

import lombok.Data;
import lombok.NonNull;

/**
 *
 */
@Data
public class MeuralPlaylists
{
	@NonNull
	private MeuralPlaylist[] data;
}
