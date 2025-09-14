package com.bigboxer23.meural_control.jwst;

import com.bigboxer23.meural_control.IMeuralImageSource;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.file.FilePersistentIndex;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlPage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Scrape content from James Webb Space Telescope site and display new content as it's published.
 */
@Slf4j
@Component
public class JWSTComponent implements IMeuralImageSource {
	private final FilePersistentIndex lastFetchedImage = new FilePersistentIndex("jwsti");

	private final FilePersistentIndex currentPage = new FilePersistentIndex("jwsti_page");

	public static final int PAGE_SIZE = 15;

	private SourceItem newestContent;

	private boolean increasing = true;

	private static final String kJWSTUrl = "https://webbtelescope.org";

	private static final List<String> skipKeywords = new ArrayList<>() {
		{
			add("spectrum");
			add("spectra");
			add("light curve");
			add("comparison");
			add("compared");
			add("compass");
		}
	};

	private final String albumToSaveTo;

	public JWSTComponent(Environment env) {
		// need to read here since we need this value before the constructor completes
		albumToSaveTo = env.getProperty("jwst-save-album");
		if (currentPage.get() == -1) {
			currentPage.set(1);
		}
		if (lastFetchedImage.get() == -1) {
			lastFetchedImage.set(0);
		}
		fetchContent();
	}

	private boolean isFileSizeAcceptable(String text) {
		boolean isSizeAcceptable = text.contains("kb")
				|| (text.contains("mb")
						&& Float.parseFloat(text.substring(text.lastIndexOf("(") + 1, text.lastIndexOf("mb") - 1))
								< 100);
		if (!isSizeAcceptable) {
			log.warn("file too large for display, " + text);
		}
		return isSizeAcceptable;
	}

	private Optional<SourceItem> findHighResLink(List<HtmlAnchor> anchors, String content) {
		return anchors.stream()
				.filter(anchor -> {
					String text = anchor.getTextContent().toLowerCase();
					return text.contains("full res")
							&& (text.contains("png") || text.contains("jpg"))
							&& isFileSizeAcceptable(text);
				})
				.findAny()
				.map(anchor -> {
					try {
						return new SourceItem(
								content + "." + FilenameUtils.getExtension(anchor.getHrefAttribute()),
								new URL("https:" + anchor.getHrefAttribute()),
								albumToSaveTo);
					} catch (MalformedURLException e) {
						log.warn("findHighResLink", e);
					}
					return null;
				});
	}

	private void fetchContent() {
		try (WebClient client = new WebClient()) {
			client.getOptions().setCssEnabled(false);
			client.getOptions().setJavaScriptEnabled(false);
			if (lastFetchedImage.get() == -1) {
				lastFetchedImage.set(PAGE_SIZE - 1);
				currentPage.set(currentPage.get() - 1);
			}
			if (currentPage.get() <= 0) {
				currentPage.set(1);
			}
			if (lastFetchedImage.get() >= PAGE_SIZE) {
				lastFetchedImage.set(0);
				currentPage.increment();
			}
			HtmlPage page = client.getPage(
					kJWSTUrl + "/resource-gallery/images?itemsPerPage=" + PAGE_SIZE + "&page=" + currentPage.get());
			List<HtmlDivision> images =
					page.getDocumentElement().getByXPath("//div[contains(@class,'ad-research-box card')]");
			if (images.isEmpty()) {
				log.warn("can't find images on page: " + currentPage.get() + " index:" + lastFetchedImage);
				if (currentPage.get() != 1) {
					currentPage.set(1);
					lastFetchedImage.set(0);
					fetchContent();
					return;
				} else {
					log.error("No images found on page 1, aborting to prevent infinite recursion");
					return;
				}
			}
			if (images.size() <= lastFetchedImage.get()) {
				log.warn("Not enough images on page "
						+ currentPage.get()
						+ ", found "
						+ images.size()
						+ " but index is "
						+ lastFetchedImage.get());
				currentPage.set(1);
				lastFetchedImage.set(0);
				fetchContent();
				return;
			}
			String content = images.get(lastFetchedImage.get()).getTextContent().trim();
			log.info(
					"Fetched JWST content: \"" + content + "\" index:" + getFetchedImageIndex() + " page:" + getPage());
			if (shouldSkipLink(content)) {
				log.info("Not showing " + content + ", matches skip keyword");
				getItem(increasing ? 1 : -1);
				return;
			}
			List<HtmlAnchor> link = images.get(lastFetchedImage.get()).getByXPath(".//a");
			if (link.isEmpty()) {
				log.warn("can't find link for content");
				return;
			}
			page = client.getPage(kJWSTUrl + link.get(0).getHrefAttribute());
			List<HtmlAnchor> downloadLinks = page.getByXPath("//a");
			if (downloadLinks.isEmpty()) {
				log.warn("can't find download links for "
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
			log.warn("JWSTComponent", e);
		}
	}

	protected boolean shouldSkipLink(String link) {
		return skipKeywords.stream().anyMatch(word -> link.toLowerCase().contains(word));
	}

	@Override
	public Optional<SourceItem> nextItem() {
		increasing = true;
		return getItem(1);
	}

	@Override
	public Optional<SourceItem> prevItem() {
		increasing = false;
		return getItem(-1);
	}

	private Optional<SourceItem> getItem(int page) {
		lastFetchedImage.set(lastFetchedImage.get() + page);
		fetchContent();
		return Optional.ofNullable(newestContent);
	}

	protected int getPage() {
		return currentPage.get();
	}

	protected void setPage(int page) {
		currentPage.set(page);
	}

	protected int getFetchedImageIndex() {
		return lastFetchedImage.get();
	}

	protected void setFetchedImageIndex(int lastFetchedImageIndex) {
		lastFetchedImage.set(lastFetchedImageIndex);
	}
}
