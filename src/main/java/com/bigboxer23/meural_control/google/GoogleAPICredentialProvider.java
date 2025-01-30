package com.bigboxer23.meural_control.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.Credentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** */
@Slf4j
@Component
public class GoogleAPICredentialProvider {
	private CredentialsProvider credProvider;

	private Credential credential;

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private static final List<String> REQUIRED_SCOPES = ImmutableList.of(
			/*"https://www.googleapis.com/auth/photoslibrary.readonly",
			"https://www.googleapis.com/auth/photoslibrary.appendonly",*/
			"https://www.googleapis.com/auth/photoslibrary", CalendarScopes.CALENDAR_READONLY);

	public JsonFactory getJsonFactory() {
		return JSON_FACTORY;
	}

	public CredentialsProvider getCredentialProvider() throws IOException, GeneralSecurityException {
		if (credProvider == null) {
			log.info("Fetching Google creds");
			InputStream aCredStream = Credentials.class.getResourceAsStream("/credentials.json");
			GoogleClientSecrets aClientSecrets =
					GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(aCredStream));
			GoogleAuthorizationCodeFlow aFlow = new GoogleAuthorizationCodeFlow.Builder(
							GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, aClientSecrets, REQUIRED_SCOPES)
					.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
					.setAccessType("offline")
					.build();
			log.info("Starting local server receiver");
			credential = new AuthorizationCodeInstalledApp(
							aFlow,
							new LocalServerReceiver.Builder().setPort(8890).build())
					.authorize("user");
			credProvider = FixedCredentialsProvider.create(UserCredentials.newBuilder()
					.setClientId(aClientSecrets.getDetails().getClientId())
					.setClientSecret(aClientSecrets.getDetails().getClientSecret())
					.setRefreshToken(credential.getRefreshToken())
					.build());
		}
		return credProvider;
	}

	public Credential getCredential() throws IOException, GeneralSecurityException {
		if (credential == null) {
			getCredentialProvider();
		}
		return credential;
	}
}
