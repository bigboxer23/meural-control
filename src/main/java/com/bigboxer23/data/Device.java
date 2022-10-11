package com.bigboxer23.data;

import lombok.Data;
import lombok.NonNull;

@Data
public class Device
{
	private String alias;

	@NonNull
	private FrameStatus frameStatus;
}
