package com.umatgong.domain.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.storage.PhotoStorageProperties;
import com.umatgong.global.storage.SupabaseStorageClient;

class PhotoServiceTest {

	private static final String BASE = "https://abcd.supabase.co";
	private static final String MINE = BASE + "/storage/v1/object/public/thumbnails/thumbs/7/";

	private final SupabaseStorageClient storage = mock(SupabaseStorageClient.class);
	private final PhotoService service = new PhotoService(
		new PhotoStorageProperties(BASE + "/", "key", "thumbnails", 524_288, 800), storage);

	@Test
	void 저장소가_설정되지_않았으면_업로드를_받지_않는다() {
		SupabaseStorageClient unused = mock(SupabaseStorageClient.class);
		PhotoService off = new PhotoService(new PhotoStorageProperties("", "", "thumbnails", 524_288, 800), unused);

		assertThatThrownBy(() -> off.uploadThumbnail(7L, new byte[] {1}))
			.isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PHOTO_STORAGE_UNAVAILABLE));
		verifyNoInteractions(unused);
		// 설정이 없으면 어떤 주소도 내 사진으로 인정하지 않는다
		assertThat(off.isOwnedBy(7L, MINE + "a.jpg")).isFalse();
	}

	@Test
	void 내_경로_바로_아래_파일만_내_사진이다() {
		assertThat(service.isOwnedBy(7L, MINE + "a.jpg")).isTrue();
		// 남의 경로, 다른 주소, 하위 폴더, 경로 넘기기
		assertThat(service.isOwnedBy(8L, MINE + "a.jpg")).isFalse();
		assertThat(service.isOwnedBy(7L, BASE + "/storage/v1/object/public/thumbnails/thumbs/77/a.jpg")).isFalse();
		assertThat(service.isOwnedBy(7L, "https://evil.test/thumbs/7/a.jpg")).isFalse();
		assertThat(service.isOwnedBy(7L, MINE + "x/a.jpg")).isFalse();
		assertThat(service.isOwnedBy(7L, MINE + "..%2F..%2Fa.jpg")).isFalse(); // ..가 들어간 주소는 통째로 거부한다
		assertThat(service.isOwnedBy(7L, MINE + "../8/a.jpg")).isFalse();
		assertThat(service.isOwnedBy(7L, null)).isFalse();
	}
}
