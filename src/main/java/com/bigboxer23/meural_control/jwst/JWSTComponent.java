package com.bigboxer23.meural_control.jwst;

import com.bigboxer23.meural_control.IMeuralImageSource;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.file.FilePersistentIndex;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Scrape content from James Webb Space Telescope Flickr album and display new content as it's
 * published.
 */
@Slf4j
@Component
public class JWSTComponent implements IMeuralImageSource {
	private final FilePersistentIndex lastFetchedImage = new FilePersistentIndex("jwsti");

	private final FilePersistentIndex currentPage = new FilePersistentIndex("jwsti_page");

	public static final int PAGE_SIZE = 15;

	private SourceItem newestContent;

	private boolean increasing = true;

	private static final String kFlickrAlbumUrl =
			"https://www.flickr.com/photos/nasawebbtelescope/albums/72177720323168468/";
	private static final String kFlickrSizesUrl = "https://www.flickr.com/photos/nasawebbtelescope/%s/sizes/o/";
	private static final String kFlickrBaseUrl = "https://www.flickr.com";

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

			String flickrAlbumUrl = kFlickrAlbumUrl + "page" + currentPage.get() + "/";
			HtmlPage page = client.getPage(flickrAlbumUrl);

			List<HtmlAnchor> photoLinks = extractPhotoLinksFromFlickrAlbum(page);

			if ((photoLinks.isEmpty() && currentPage.get() != 1) // prevent looping
					|| photoLinks.size() <= lastFetchedImage.get()) {
				if (photoLinks.isEmpty()) {
					log.warn("can't find images on page: " + currentPage.get() + " index:" + lastFetchedImage);
				}
				currentPage.set(1);
				lastFetchedImage.set(0);
				fetchContent();
				return;
			}

			HtmlAnchor photoLink = photoLinks.get(lastFetchedImage.get());
			String photoId = extractPhotoIdFromLink(photoLink.getHrefAttribute());
			String photoTitle = photoLink.getAttribute("title");

			if (photoTitle == null || photoTitle.trim().isEmpty()) {
				photoTitle = "JWST Image " + photoId;
			}

			log.info("Fetched JWST content: \""
					+ photoTitle
					+ "\" index:"
					+ getFetchedImageIndex()
					+ " page:"
					+ getPage());

			if (shouldSkipLink(photoTitle)) {
				log.info("Not showing " + photoTitle + ", matches skip keyword");
				getItem(increasing ? 1 : -1);
				return;
			}

			Optional<URL> downloadUrl = getFlickrDownloadUrl(client, photoId);
			if (downloadUrl.isEmpty()) {
				log.warn("can't find download link for photo ID: " + photoId);
				return;
			}

			newestContent = new SourceItem(photoTitle + ".jpg", downloadUrl.get(), albumToSaveTo);

		} catch (IOException e) {
			log.warn("JWSTComponent", e);
		}
	}

	protected boolean shouldSkipLink(String link) {
		return skipKeywords.stream().anyMatch(word -> link.toLowerCase().contains(word));
	}

	private List<HtmlAnchor> extractPhotoLinksFromFlickrAlbum(HtmlPage page) {
		List<HtmlAnchor> photoLinks = page.getByXPath("//a[contains(@href, '/photos/nasawebbtelescope/')]");

		return photoLinks.stream()
				.filter(anchor -> anchor.getHrefAttribute().matches(".*/photos/nasawebbtelescope/\\d+/?.*"))
				.toList();
	}

	private String extractPhotoIdFromLink(String href) {

		String[] parts = href.split("/");
		for (String part : parts) {
			if (part.matches("\\d+")) {
				return part;
			}
		}
		throw new IllegalArgumentException("Could not extract photo ID from URL: " + href);
	}

	private Optional<URL> getFlickrDownloadUrl(WebClient client, String photoId) {
		try {
			String sizesUrl = String.format(kFlickrSizesUrl, photoId);
			HtmlPage sizesPage = client.getPage(sizesUrl);

			List<HtmlAnchor> downloadLinks = sizesPage.getByXPath("//a[contains(@href, 'photo_download.gne')]");

			for (HtmlAnchor link : downloadLinks) {
				String href = link.getHrefAttribute();
				if (href.contains("size=o") && href.contains("id=" + photoId)) {
					if (href.startsWith("/")) {
						href = kFlickrBaseUrl + href;
					} else if (!href.startsWith("http")) {
						href = kFlickrBaseUrl + "/" + href;
					}
					return Optional.of(new URL(href));
				}
			}

			List<HtmlAnchor> allLinks = sizesPage.getByXPath("//a");
			for (HtmlAnchor link : allLinks) {
				String linkText = link.getTextContent().toLowerCase();
				String href = link.getHrefAttribute();
				if ((linkText.contains("download") || linkText.contains("original")) && href.contains(photoId)) {
					if (href.startsWith("/")) {
						href = kFlickrBaseUrl + href;
					} else if (!href.startsWith("http")) {
						href = kFlickrBaseUrl + "/" + href;
					}
					return Optional.of(new URL(href));
				}
			}

		} catch (IOException e) {
			log.warn("Error fetching download URL for photo ID: " + photoId, e);
		}

		return Optional.empty();
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
