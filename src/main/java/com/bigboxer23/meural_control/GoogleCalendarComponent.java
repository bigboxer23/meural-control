package com.bigboxer23.meural_control;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

/**
 * Component to interrogate a Google calendar for holiday information
 */
@Component
public class GoogleCalendarComponent
{
	private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarComponent.class);

	private final GoogleAPICredentialProvider credentialProviderComponent;

	private String holidayString;

	private File holidayFile = new File(System.getProperty("user.dir") + File.separator + "holiday");

	public GoogleCalendarComponent(GoogleAPICredentialProvider credentialComponent)
	{
		credentialProviderComponent = credentialComponent;
		holidayString = "";
		if (holidayFile.exists())
		{
			try
			{
				holidayString = FileUtils.readFileToString(holidayFile, Charset.defaultCharset());
			} catch (IOException e)
			{
				logger.warn("error reading prompt", e);
			}
		}

	}

	@Scheduled(cron = "0 0 0 ? * *")//Run every day at 12am
	public void updateHoliday()
	{
		logger.info("Fetching holiday information");
		try
		{
			holidayString = "";
			Calendar aCalendar = new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
					credentialProviderComponent.getJsonFactory(), credentialProviderComponent.getCredential())
					.setApplicationName("Meural")
					.build();
			aCalendar.events().list("en.usa#holiday@group.v.calendar.google.com")
					.setMaxResults(25)
					.setTimeMin(new DateTime(System.currentTimeMillis()))
					.setTimeMax(new DateTime(System.currentTimeMillis() + (86400000 * 7)))//+1 day * 7 days
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute()
					.getItems()
					.stream()
					.findAny()
					.ifPresent(event -> holidayString = " " + event.getSummary());
			logger.info("holiday string:" + holidayString);
			try
			{
				FileUtils.writeStringToFile(holidayFile, holidayString, Charset.defaultCharset(), false);
			} catch (IOException e)
			{
				logger.warn("error writing prompt", e);
			}
		}
		catch (GeneralSecurityException | IOException e)
		{
			logger.warn("updateHoliday", e);
		}
	}

	public String getHolidayString()
	{
		return holidayString;
	}
}
