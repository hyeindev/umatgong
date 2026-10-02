package com.umatgong.domain.photo.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.umatgong.domain.photo.dto.ThumbnailResponse;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.storage.PhotoStorageProperties;
import com.umatgong.global.storage.SupabaseStorageClient;

import lombok.RequiredArgsConstructor;

/**
 * 썸네일 업로드. <b>원본 사진은 받지 않는다</b> (기획서). 프론트가 줄인 썸네일만 받고,
 * 긴 변이 maxDimension보다 크면 원본으로 보고 거부한다.
 *
 * <p>받은 그림은 한 번 풀어서 JPEG로 다시 저장한다. 그래서 촬영 위치(GPS)를 포함한 EXIF 같은 메타데이터가
 * 저장소에 남지 않고, 그림이 아닌 파일은 여기서 걸러진다.
 *
 * <p>경로는 thumbs/{사용자 ID}/{무작위}.jpg다. 기록에는 내 경로 아래 주소만 붙일 수 있다 ({@link #isOwnedBy}).
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

	private static final String DIR = "thumbs";

	private final PhotoStorageProperties properties;
	private final SupabaseStorageClient storage;

	public ThumbnailResponse uploadThumbnail(Long userId, byte[] bytes) {
		if (!properties.configured()) {
			throw new BusinessException(ErrorCode.PHOTO_STORAGE_UNAVAILABLE);
		}
		if (bytes.length == 0 || bytes.length > properties.maxBytes()) {
			throw new BusinessException(ErrorCode.PHOTO_INVALID,
				"썸네일은 " + properties.maxBytes() / 1024 + "KB 이하여야 합니다.");
		}
		BufferedImage image = decode(bytes);
		int longSide = Math.max(image.getWidth(), image.getHeight());
		if (longSide > properties.maxDimension()) {
			throw new BusinessException(ErrorCode.PHOTO_INVALID,
				"썸네일은 긴 변이 " + properties.maxDimension() + "px 이하여야 합니다. 원본은 올리지 않습니다.");
		}
		byte[] jpeg = reencode(image);
		String path = ownedPrefix(userId) + UUID.randomUUID() + ".jpg";
		storage.upload(path, jpeg, MediaType.IMAGE_JPEG);
		return new ThumbnailResponse(properties.publicUrl(path), image.getWidth(), image.getHeight());
	}

	/** 이 사용자가 올린 썸네일 주소인지. 남이 올린 사진이나 바깥 주소를 기록에 붙이지 못하게 한다 */
	public boolean isOwnedBy(Long userId, String url) {
		if (!properties.configured() || url == null) {
			return false;
		}
		String prefix = properties.publicUrl(ownedPrefix(userId));
		return url.startsWith(prefix) && !url.substring(prefix.length()).contains("/")
			&& !url.contains("..");
	}

	/** 기록을 지운 뒤 그 썸네일도 저장소에서 지운다 (공개 주소가 남아 있지 않게) */
	public void deleteQuietly(Collection<String> urls) {
		String publicPrefix = properties.publicUrl("");
		List<String> paths = urls.stream()
			.filter(url -> url.startsWith(publicPrefix))
			.map(url -> url.substring(publicPrefix.length()))
			.toList();
		storage.deleteQuietly(paths);
	}

	private static String ownedPrefix(Long userId) {
		return DIR + "/" + userId + "/";
	}

	private static BufferedImage decode(byte[] bytes) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null) {
				throw new BusinessException(ErrorCode.PHOTO_INVALID, "JPEG나 PNG 그림만 올릴 수 있습니다.");
			}
			return image;
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.PHOTO_INVALID, "그림을 읽을 수 없습니다.");
		}
	}

	// 투명한 PNG는 JPEG에 알파가 없으므로 어두운 바탕에 얹는다 (지도·카드 바탕과 비슷한 색)
	private static byte[] reencode(BufferedImage source) {
		BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		try {
			g.setColor(new Color(0x1d, 0x1d, 0x1b));
			g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
			g.drawImage(source, 0, 0, null);
		} finally {
			g.dispose();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			if (!ImageIO.write(rgb, "jpg", out)) {
				throw new IllegalStateException("No JPEG writer");
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return out.toByteArray();
	}
}
