package com.bigboxer23.meural_control.jwst;

import com.bigboxer23.meural_control.IMeuralImageSource;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.file.FilePersistedString;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scrape content from James Webb Space Telescope site and display new content as it's published.
 */
@Component
public class JWSTComponent implements IMeuralImageSource {
	private static final Logger logger = LoggerFactory.getLogger(JWSTComponent.class);

	private final FilePersistedString lastFetched = new FilePersistedString("jwst");

	private SourceItem newestContent;

	private static final String kJWSTUrl = "https://webbtelescope.org";

	@Value("${jwst-save-album}")
	private String albumToSaveTo;

	public JWSTComponent() {
		checkForNewContent();
	}

	private SourceItem findHighResLink(List<HtmlAnchor> anchors, String content) {
		return anchors.stream()
				.filter(anchor -> {
					String text = anchor.getTextContent().toLowerCase();
					return text.contains("full res.") && (text.contains("png") || text.contains("jpg"));
				})
				.findAny()
				.map(anchor -> {
					try {
						return new SourceItem(
								content + "." + FilenameUtils.getExtension(anchor.getHrefAttribute()),
								new URL("https:" + anchor.getHrefAttribute()),
								albumToSaveTo);
					} catch (MalformedURLException e) {
						logger.warn("findHighResLink", e);
					}
					return null;
				})
				.orElse(null);
	}

	@Scheduled(cron = "0 0 0 ? * *")
	private void checkForNewContent() {
		try (WebClient client = new WebClient()) {
			client.getOptions().setCssEnabled(false);
			client.getOptions().setJavaScriptEnabled(false);
			HtmlPage page = client.getPage(kJWSTUrl + "/resource-gallery/images");
			List<HtmlDivision> images =
					page.getDocumentElement().getByXPath("//div[contains(@class,'ad-research-box')]");
			if (images.isEmpty()) {
				logger.warn("can't find images on page");
				return;
			}
			String content = images.get(0).getTextContent().trim();
			if (newestContent != null
					&& !lastFetched.get().isBlank()
					&& lastFetched.get().equals(content)) {
				logger.debug("previous content is already displayed");
				return;
			}
			logger.info("Found new JWST content: " + content);
			List<HtmlAnchor> link = images.get(0).getByXPath("a");
			if (link.isEmpty()) {
				logger.warn("can't find link for content");
				return;
			}
			page = client.getPage(kJWSTUrl + link.get(0).getHrefAttribute());
			List<HtmlAnchor> downloadLinks = page.getByXPath("//a");
			if (downloadLinks.isEmpty()) {
				logger.warn("can't find download links for "
						+ kJWSTUrl
						+ link.get(0).getHrefAttribute());
				return;
			}
			newestContent = findHighResLink(downloadLinks, content);
			if (newestContent != null) {
				lastFetched.set(content);
			}
		} catch (IOException e) {
			logger.warn("JWSTComponent", e);
		}
	}

	@Override
	public Optional<SourceItem> nextItem() {
		return Optional.ofNullable(newestContent);
	}

	@Override
	public Optional<SourceItem> prevItem() {
		return Optional.ofNullable(newestContent);
	}
}
