package com.bigboxer23.meural_control.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Utility class to persist an index to a file on disk and resume from it on restart of program.
 */
public class FilePersistentIndex
{
	private static final Logger logger = LoggerFactory.getLogger(FilePersistentIndex.class);

	private int currentIndex = -1;

	private String filePath;

	public FilePersistentIndex(String fileName)
	{
		this.filePath = System.getProperty("user.dir") + File.separator + fileName;
		try
		{
			currentIndex = Integer.parseInt(FileUtils.readFileToString(new File(this.filePath), Charset.defaultCharset()));
		} catch (NumberFormatException e)
		{
			logger.warn("FilePersistentIndex", e);
		} catch (IOException e) {}
		logger.info(fileName + ": initialized with value " + currentIndex);
	}

	private void writeToFile()
	{
		try
		{
			FileUtils.writeStringToFile(new File(this.filePath), currentIndex + "", Charset.defaultCharset(), false);
		} catch (IOException e)
		{
			logger.warn("FilePersistentIndex:writeToFile", e);
		}
	}

	public int get()
	{
		return currentIndex;
	}

	public int reset()
	{
		currentIndex = -1;
		writeToFile();
		return currentIndex;
	}

	public int increment()
	{
		currentIndex++;
		return set(currentIndex);
	}

	public int set(int newValue)
	{
		currentIndex = newValue;
		writeToFile();
		return currentIndex;
	}
}
