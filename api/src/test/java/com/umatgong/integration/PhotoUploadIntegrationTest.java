package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.security.JwtProvider;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 썸네일 업로드를 HTTP → 서버(검사·재인코딩) → Supabase Storage(MockWebServer)까지 흘린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class PhotoUploadIntegrationTest {

	private static final String KEY = "test-service-key";
	private static final List<RecordedRequest> STORAGE_REQUESTS = Collections.synchronizedList(new ArrayList<>());
	private static volatile Function<RecordedRequest, MockResponse> storageResponder = r -> ok();
	private static final MockWebServer STORAGE = start();

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) {
		registry.add("umatgong.photo.supabase-url", () -> STORAGE.url("/").toString());
		registry.add("umatgong.photo.service-key", () -> KEY);
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtProvider jwtProvider;

	private long userId;
	private String token;

	@BeforeEach
	void setUp() {
		STORAGE_REQUESTS.clear();
		storageResponder = r -> ok();
		userId = userRepository.save(User.signUpWithKakao(1L, "지현", null)).getId();
		token = jwtProvider.createAccessToken(userId);
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@Test
	void 썸네일을_올리면_내_경로에_JPEG로_저장되고_공개_주소가_온다() throws Exception {
		JsonNode thumb = data(upload("t.jpg", "image/jpeg", withExif(jpeg(400, 300)))
			.andExpect(status().isCreated()));

		String prefix = STORAGE.url("/").toString().replaceAll("/$", "")
			+ "/storage/v1/object/public/thumbnails/thumbs/" + userId + "/";
		assertThat(thumb.get("url").asText()).startsWith(prefix).endsWith(".jpg");
		assertThat(thumb.get("width").asInt()).isEqualTo(400);
		assertThat(thumb.get("height").asInt()).isEqualTo(300);

		assertThat(STORAGE_REQUESTS).hasSize(1);
		RecordedRequest put = STORAGE_REQUESTS.get(0);
		assertThat(put.getMethod()).isEqualTo("POST");
		assertThat(put.getUrl().encodedPath()).startsWith("/storage/v1/object/thumbnails/thumbs/" + userId + "/");
		assertThat(put.getHeaders().get(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + KEY);
		assertThat(put.getHeaders().get("apikey")).isEqualTo(KEY);
		assertThat(put.getHeaders().get("x-upsert")).isEqualTo("false");
		assertThat(put.getHeaders().get(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/jpeg");
		byte[] stored = put.getBody().toByteArray();
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored));
		assertThat(decoded.getWidth()).isEqualTo(400);
		// 다시 저장했으므로 촬영 위치 같은 EXIF가 남지 않는다
		assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("Exif").doesNotContain("SECRET-GPS");
	}

	@Test
	void 투명한_PNG도_JPEG로_바꿔_저장한다() throws Exception {
		BufferedImage png = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
		data(upload("t.png", "image/png", encode(png, "png")).andExpect(status().isCreated()));

		assertThat(STORAGE_REQUESTS.get(0).getHeaders().get(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/jpeg");
	}

	@Test
	void 긴_변이_800px를_넘으면_원본으로_보고_받지_않는다() throws Exception {
		upload("big.jpg", "image/jpeg", jpeg(1200, 900))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PHOTO_INVALID"));
		assertThat(STORAGE_REQUESTS).isEmpty();
	}

	@Test
	void 크기_제한을_넘거나_그림이_아니면_받지_않는다() throws Exception {
		BufferedImage noise = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
		Random random = new Random(1);
		for (int x = 0; x < 600; x++) {
			for (int y = 0; y < 600; y++) {
				noise.setRGB(x, y, random.nextInt());
			}
		}
		byte[] heavy = encode(noise, "png");
		assertThat(heavy.length).isGreaterThan(524_288);
		upload("heavy.png", "image/png", heavy)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PHOTO_INVALID"));

		upload("note.jpg", "image/jpeg", "not an image".getBytes(StandardCharsets.UTF_8))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PHOTO_INVALID"));
		upload("empty.jpg", "image/jpeg", new byte[0]).andExpect(status().isBadRequest());

		assertThat(STORAGE_REQUESTS).isEmpty();
	}

	@Test
	void 저장소가_실패하면_503() throws Exception {
		storageResponder = r -> new MockResponse.Builder().code(500).body("{}").build();

		upload("t.jpg", "image/jpeg", jpeg(100, 100))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error.code").value("PHOTO_STORAGE_UNAVAILABLE"));
	}

	@Test
	void 로그인하지_않으면_401이고_저장소를_부르지_않는다() throws Exception {
		mockMvc.perform(multipart("/api/photos/thumbnails")
				.file(new MockMultipartFile("file", "t.jpg", "image/jpeg", jpeg(100, 100))))
			.andExpect(status().isUnauthorized());
		assertThat(STORAGE_REQUESTS).isEmpty();
	}

	@Test
	void 올린_사진으로_기록하고_기록을_지우면_저장소에서도_지운다() throws Exception {
		String url = data(upload("t.jpg", "image/jpeg", jpeg(100, 100))).get("url").asText();
		long placeId = jdbc.queryForObject("""
			insert into places (name, coordinate, source, external_id)
			values ('망원동 김반장', ST_SetSRID(ST_MakePoint(126.9236, 37.5563), 4326)::geography, 'API', 'k-1')
			returning id""", Long.class);
		String body = "{\"placeId\":" + placeId + ",\"rating\":\"AGAIN\",\"visitedAt\":\"2026-09-30T12:00:00Z\","
			+ "\"thumbnailUrls\":[\"" + url + "\"]}";
		long visitId = data(mockMvc.perform(post("/api/visits").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())).get("id").asLong();

		mockMvc.perform(delete("/api/visits/" + visitId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());

		RecordedRequest removal = STORAGE_REQUESTS.get(STORAGE_REQUESTS.size() - 1);
		assertThat(removal.getMethod()).isEqualTo("DELETE");
		assertThat(removal.getUrl().encodedPath()).isEqualTo("/storage/v1/object/thumbnails");
		String path = url.substring(url.indexOf("/thumbs/") + 1);
		assertThat(objectMapper.readTree(removal.getBody().utf8()).get("prefixes").get(0).asText()).isEqualTo(path);
	}

	// ── 도우미 ──

	private ResultActions upload(String name, String type, byte[] bytes) throws Exception {
		return mockMvc.perform(multipart("/api/photos/thumbnails")
			.file(new MockMultipartFile("file", name, type, bytes))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private JsonNode data(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
	}

	private static byte[] jpeg(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0xd4, 0xf5, 0x3c));
		g.fillRect(0, 0, width / 2, height);
		g.dispose();
		return encode(image, "jpg");
	}

	private static byte[] encode(BufferedImage image, String format) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, format, out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// SOI 바로 뒤에 최소 EXIF(APP1) 세그먼트를 끼워 넣는다. 끝에 알아볼 표식을 붙여 남는지 확인한다
	private static byte[] withExif(byte[] jpeg) {
		byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 0};
		byte[] marker = "SECRET-GPS".getBytes(StandardCharsets.US_ASCII);
		byte[] header = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
		int length = 2 + header.length + tiff.length + marker.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(jpeg, 0, 2);
		out.write(0xFF);
		out.write(0xE1);
		out.write(length >> 8);
		out.write(length & 0xFF);
		out.writeBytes(header);
		out.writeBytes(tiff);
		out.writeBytes(marker);
		out.write(jpeg, 2, jpeg.length - 2);
		byte[] result = out.toByteArray();
		assertThat(new String(result, StandardCharsets.ISO_8859_1)).contains("SECRET-GPS");
		return result;
	}

	private static MockResponse ok() {
		return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
			.body("{\"Key\":\"ok\"}").build();
	}

	private static MockWebServer start() {
		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				STORAGE_REQUESTS.add(request);
				return storageResponder.apply(request);
			}
		});
		try {
			server.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return server;
	}
}
