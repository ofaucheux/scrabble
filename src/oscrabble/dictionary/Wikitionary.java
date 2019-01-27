package oscrabble.dictionary;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOError;
import java.io.IOException;
import java.net.URL;

public class Wikitionary implements WordMetainformationProvider
{
	public final static Logger LOGGER = Logger.getLogger(Wikitionary.class);

	private final String serverUrl;
	int width = 0;

	public Wikitionary(final String serverUrl)
	{
		this.serverUrl = serverUrl;
	}

	@Override
	public String getDescription(final String word)
	{
		// https://en.wiktionary.org/w/index.php?title=test&printable=yes
		try
		{
			String content = IOUtils.toString(
					new URL(serverUrl + "/w/index.php?title=" + word.toLowerCase() + "&printable=yes")
			);
			content = content.replaceAll("^<!DOCTYPE html>", "");
			content = content.replaceAll("^\\s*<html[^>]*>", "<html>");

			if (this.width > 1)
			{
				content = content.replaceFirst("<body ", "<body style='width: " + width + "px'");
			}

			return content;
		}
		catch (IOException e)
		{
			LOGGER.error("Cannot find " + word, e);
			return "";
		}
	}

	public void setHtmlWidth(final int width)
	{
		this.width = width;
	}

}
