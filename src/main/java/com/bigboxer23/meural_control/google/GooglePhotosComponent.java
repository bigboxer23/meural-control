package com.bigboxer23.meural_control.google;

import com.bigboxer23.meural_control.IMeuralImageSource;
import com.bigboxer23.meural_control.data.SourceItem;
import com.bigboxer23.utils.file.FilePersistentIndex;
import com.google.api.gax.rpc.ApiException;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component to interface with Google Photos so content can be pushed direct (and live from cloud)
 * to the Meural device without needing to depend on Meural services.
 */
@Component
public class GooglePhotosComponent implements IMeuralImageSource {
	private static final Logger logger = LoggerFactory.getLogger(GooglePhotosComponent.class);

	@Value("${gPhotos-albumTitle}")
	private String albumTitle;

	private String albumId;

	private final GoogleAPICredentialProvider credentialProviderComponent;

	private final FilePersistentIndex currentItem = new FilePersistentIndex("gPhotosIndex");

	public GooglePhotosComponent(GoogleAPICredentialProvider credentialComponent) {
		credentialProviderComponent = credentialComponent;
	}

	@Override
	public Optional<SourceItem> nextItem() {
		return jumpToItem(1);
	}

	@Override
	public Optional<SourceItem> prevItem() {
		return jumpToItem(-1);
	}

	/** Iterate through an album's content and */
	private Optional<SourceItem> jumpToItem(int jump) {
		currentItem.set(currentItem.get() + jump);
		try {
			PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
					.setCredentialsProvider(credentialProviderComponent.getCredentialProvider())
					.build();
			try (PhotosLibraryClient photosLibraryClient = PhotosLibraryClient.initialize(settings)) {
				InternalPhotosLibraryClient.SearchMediaItemsPagedResponse aResults =
						photosLibraryClient.searchMediaItems(findAlbumId(photosLibraryClient));
				Iterator<MediaItem> items = aResults.iterateAll().iterator();
				for (int ai = 0; items.hasNext(); ai++) {
					MediaItem item = items.next();
					if (ai == currentItem.get()) {
						logger.info("returning item " + currentItem.get() + " from album " + albumTitle);
						return Optional.of(
								new SourceItem(item.getFilename(), new URL(item.getBaseUrl() + "=w10000-h10000")));
					}
				}
				currentItem.reset();
				return jumpToItem(1);
			}
		} catch (IOException | GeneralSecurityException theE) {
			logger.warn("nextItem:", theE);
		}
		return Optional.empty();
	}

	private String findAlbumId(PhotosLibraryClient client) {
		if (albumId == null) {
			albumId = findOrCreateAlbumId(albumTitle, false, client);
		}
		return albumId;
	}

	public void changeAlbum(String albumTitle) {
		albumId = null;
		currentItem.reset();
		this.albumTitle = albumTitle;
	}

	protected void removeItemFromAlbum(String albumName, MediaItem item) {

		logger.info("removing item from google photos album: \"" + item.getFilename() + "\"");
		try {
			PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
					.setCredentialsProvider(credentialProviderComponent.getCredentialProvider())
					.build();
			try (PhotosLibraryClient photosLibraryClient = PhotosLibraryClient.initialize(settings)) {
				photosLibraryClient.batchRemoveMediaItemsFromAlbum(
						findOrCreateAlbumId(albumName, true, photosLibraryClient),
						Collections.singletonList(item.getId()));
			}
		} catch (IOException | GeneralSecurityException | ApiException theE) {
			logger.warn("removeItemFromAlbum:", theE);
		}
	}

	public NewMediaItemResult uploadItemToAlbum(SourceItem item) {
		logger.info("uploading item to google photos album: \"" + item.getName() + "\"");
		try {
			PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
					.setCredentialsProvider(credentialProviderComponent.getCredentialProvider())
					.build();
			try (PhotosLibraryClient photosLibraryClient = PhotosLibraryClient.initialize(settings);
					RandomAccessFile file = new RandomAccessFile(item.getTempFile(), "r")) {
				UploadMediaItemRequest request = UploadMediaItemRequest.newBuilder()
						.setMimeType("image/png")
						.setDataFile(file)
						.build();
				UploadMediaItemResponse response = photosLibraryClient.uploadMediaItem(request);
				if (response.getError().isPresent()) {
					UploadMediaItemResponse.Error error = response.getError().get();
					logger.warn("uploadUrlToAlbum error", error.getCause());
					return null;
				}
				if (response.getUploadToken().isEmpty()) {
					logger.warn("uploadUrlToAlbum no upload token exists");
					return null;
				}
				String uploadToken = response.getUploadToken().get();
				NewMediaItem newMediaItem = NewMediaItemFactory.createNewMediaItem(
						uploadToken, StringUtils.truncate(item.getName(), 254), item.getName());
				BatchCreateMediaItemsResponse createItemsResponse = photosLibraryClient.batchCreateMediaItems(
						findOrCreateAlbumId(item.getAlbumToSaveTo(), true, photosLibraryClient),
						Collections.singletonList(newMediaItem));
				for (NewMediaItemResult itemsResponse : createItemsResponse.getNewMediaItemResultsList()) {
					Status status = itemsResponse.getStatus();
					if (status.getCode() != Code.OK_VALUE) {
						logger.warn("error creating media item: " + status.getCode() + " " + status.getMessage());
					}
					return itemsResponse;
				}
			}
		} catch (IOException | GeneralSecurityException | ApiException theE) {
			logger.warn("uploadUrlToAlbum:", theE);
		}
		return null;
	}

	private String findOrCreateAlbumId(
			String albumTitle, boolean excludeNonAppCreatedAlbum, PhotosLibraryClient client) {
		for (Album album : client.listAlbums(excludeNonAppCreatedAlbum).iterateAll()) {
			if (albumTitle.equalsIgnoreCase(album.getTitle())) {
				return album.getId();
			}
		}
		Album createdAlbum = client.createAlbum(albumTitle);
		return createdAlbum.getId();
	}

	protected int getImageIndex() {
		return currentItem.get();
	}

	protected void setImageIndex(int lastFetchedImageIndex) {
		currentItem.set(lastFetchedImageIndex);
	}
}
