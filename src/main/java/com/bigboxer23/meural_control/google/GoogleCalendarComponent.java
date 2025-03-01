package com.bigboxer23.meural_control.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Component to interrogate a Google calendar for holiday information */
@Slf4j
@Component
public class GoogleCalendarComponent {
	private final GoogleAPICredentialProvider credentialProviderComponent;

	private String holidayString;

	public GoogleCalendarComponent(GoogleAPICredentialProvider credentialComponent) {
		credentialProviderComponent = credentialComponent;
		updateHoliday();
	}

	@Scheduled(cron = "0 0 0 ? * *") // Run every day at 12am
	public void updateHoliday() {
		log.info("Fetching holiday information");
		try {
			holidayString = "";
			Calendar aCalendar = new Calendar.Builder(
							GoogleNetHttpTransport.newTrustedTransport(),
							credentialProviderComponent.getJsonFactory(),
							credentialProviderComponent.getCredential())
					.setApplicationName("Meural")
					.build();
			aCalendar
					.events()
					.list("en.usa#holiday@group.v.calendar.google.com")
					.setMaxResults(25)
					.setTimeMin(new DateTime(System.currentTimeMillis()))
					.setTimeMax(new DateTime(System.currentTimeMillis() + (86400000 * 7))) // +1 day * 7 days
					.setOrderBy("startTime")
					.setSingleEvents(true)
					.execute()
					.getItems()
					.stream()
					.findAny()
					.ifPresent(event -> holidayString = " with elements of " + event.getSummary());
			log.info("holiday string:" + holidayString);
		} catch (GeneralSecurityException | IOException e) {
			log.warn("updateHoliday", e);
		}
	}

	public String getHolidayString() {
		return holidayString;
	}
}
