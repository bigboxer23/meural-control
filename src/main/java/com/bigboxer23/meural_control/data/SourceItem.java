package com.bigboxer23.meural_control.data;

import lombok.Data;

import java.io.File;
import java.net.URL;

/**
 * encapsulate a file name and URL since some sources don't embed the real name properly
 */
@Data
public class SourceItem
{
	public SourceItem(String filename, URL url, String albumToSaveTo)
	{
		this.url = url;
		this.name = filename;
		this.albumToSaveTo = albumToSaveTo;
	}

	public SourceItem(String filename, URL url)
	{
		this(filename, url, null);
	}

	private URL url;

	private String name;

	private String albumToSaveTo;

	private File tempFile;
}
