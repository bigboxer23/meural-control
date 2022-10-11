package com.bigboxer23.data;

import lombok.Data;
import lombok.NonNull;

@Data
public class Devices
{
	@NonNull
	private Device[] data;
}
