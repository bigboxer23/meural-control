package com.bigboxer23.meural_control.data;

import lombok.Data;

import java.net.URL;

/**
 * encapsulate a file name and URL since some sources don't embed the real name properly
 */
@Data
public class SourceItem
{
	public SourceItem(String filename, URL url)
	{
		this.url = url;
		this.name = filename;
	}

	private URL url;

	private String name;
}
