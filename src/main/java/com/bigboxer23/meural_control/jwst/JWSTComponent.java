package com.bigboxer23.meural_control.jwst;

import com.bigboxer23.meural_control.IMeuralImageSource;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.file.FilePersistentIndex;
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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Scrape content from James Webb Space Telescope site and display new content as it's published.
 */
@Component
public class JWSTComponent implements IMeuralImageSource {
	private static final Logger logger = LoggerFactory.getLogger(JWSTComponent.class);

	private final FilePersistentIndex lastFetchedImage = new FilePersistentIndex("jwsti");
	private SourceItem newestContent;

	private static final String kJWSTUrl = "https://webbtelescope.org";

	private final String albumToSaveTo;

	public JWSTComponent(Environment env) {
		// need to read here since we need this value before the constructor completes
		albumToSaveTo = env.getProperty("jwst-save-album");
		fetchContent();
	}

	private Optional<SourceItem> findHighResLink(List<HtmlAnchor> anchors, String content) {
		return anchors.stream()
				.filter(anchor -> {
					String text = anchor.getTextContent().toLowerCase();
					return text.contains("full res") && (text.contains("png") || text.contains("jpg"));
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
				});
	}

	private void fetchContent() {
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
			if (lastFetchedImage.get() == -1) {
				lastFetchedImage.set(images.size() - 1);
			}
			if (images.size() <= lastFetchedImage.get()) {
				lastFetchedImage.set(0);
			}
			String content = images.get(lastFetchedImage.get()).getTextContent().trim();
			logger.info("Fetched JWST content: " + content);
			List<HtmlAnchor> link = images.get(lastFetchedImage.get()).getByXPath("a");
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
			newestContent = findHighResLink(downloadLinks, content)
					.orElseThrow(() -> new IOException("can't fetch image from: \""
							+ kJWSTUrl
							+ link.get(0).getHrefAttribute()
							+ "\""));
		} catch (IOException e) {
			logger.warn("JWSTComponent", e);
		}
	}

	@Override
	public Optional<SourceItem> nextItem() {
		return getItem(1);
	}

	@Override
	public Optional<SourceItem> prevItem() {
		return getItem(-1);
	}

	private Optional<SourceItem> getItem(int page) {
		lastFetchedImage.set(lastFetchedImage.get() + page);
		fetchContent();
		return Optional.ofNullable(newestContent);
	}
}
