package com.umatgong.global.kakao;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.geo.Coordinate;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 로컬 API(장소 검색) 전담. REST API 키는 이 클래스와 KakaoAuthClient(웹 로그인 코드 교환)만 쓴다.
 *
 * <p><b>카카오는 x가 경도, y가 위도다.</b> 이 클래스 밖은 전부 {@link Coordinate}(lat, lng)로만 다루고,
 * x/y 변환은 요청을 만들 때({@link #withCenter})와 응답을 읽을 때({@link #toPlace}) 딱 두 곳에서만 한다.
 */
@Slf4j
@Component
public class KakaoLocalClient {

	private static final String CATEGORY_PATH = "/v2/local/search/category.json";
	private static final String KEYWORD_PATH = "/v2/local/search/keyword.json";
	// 카카오가 한 페이지에 주는 최대 개수. 호출 수를 늘리지 않으려고 첫 페이지만 쓴다.
	private static final int PAGE_SIZE = 15;

	private final RestClient restClient;
	// "KakaoAK {키}". 필드로만 들고 있고 밖으로 돌려주는 메서드는 없다.
	private final String authorizationHeader;

	public KakaoLocalClient(@Qualifier("kakaoLocalRestClient") RestClient restClient, KakaoProperties properties) {
		String key = properties.restApiKey();
		if (key == null || key.isBlank()) {
			throw new IllegalStateException("umatgong.kakao.rest-api-key (KAKAO_REST_API_KEY) must be set");
		}
		this.restClient = restClient;
		this.authorizationHeader = "KakaoAK " + key;
	}

	/** 중심 좌표 반경 안의 카테고리 장소. 가까운 순. */
	public List<KakaoPlace> searchByCategory(KakaoCategory category, Coordinate center, int radiusMeters) {
		return search("GET " + CATEGORY_PATH + " " + category.code(), uri -> withCenter(
			uri.path(CATEGORY_PATH)
				.queryParam("category_group_code", category.code())
				.queryParam("radius", radiusMeters)
				.queryParam("sort", "distance")
				.queryParam("size", PAGE_SIZE),
			center).build());
	}

	/**
	 * 키워드 장소 검색. 정확도 순.
	 *
	 * @param center 있으면 카카오가 거리를 계산할 기준. 없으면 null
	 */
	public List<KakaoPlace> searchByKeyword(String query, Coordinate center) {
		return search("GET " + KEYWORD_PATH, uri -> {
			UriBuilder builder = uri.path(KEYWORD_PATH)
				.queryParam("query", query)
				.queryParam("size", PAGE_SIZE);
			return (center == null ? builder : withCenter(builder, center)).build();
		});
	}

	// 우리 좌표 → 카카오: x에 경도, y에 위도.
	private static UriBuilder withCenter(UriBuilder uri, Coordinate center) {
		return uri
			.queryParam("x", plain(center.lng()))
			.queryParam("y", plain(center.lat()));
	}

	// 카카오 → 우리 좌표: y가 위도, x가 경도.
	private static KakaoPlace toPlace(Document d) {
		Coordinate coordinate = Coordinate.of(Double.parseDouble(d.y()), Double.parseDouble(d.x()));
		String address = d.roadAddressName() == null || d.roadAddressName().isBlank()
			? d.addressName() : d.roadAddressName();
		return new KakaoPlace(d.id(), d.placeName(), address, d.categoryName(), d.categoryGroupCode(), coordinate);
	}

	private List<KakaoPlace> search(String logLabel, Function<UriBuilder, URI> uri) {
		long start = System.nanoTime();
		try {
			SearchResponse response = restClient.get()
				.uri(uri)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.retrieve()
				.body(SearchResponse.class);
			List<KakaoPlace> places = response == null || response.documents() == null
				? List.of()
				: response.documents().stream().map(KakaoLocalClient::toPlace).toList();
			// 호출 건수가 곧 비용이다. 모든 카카오 호출을 같은 형식("kakao call")으로 남겨 집계할 수 있게 한다.
			log.info("kakao call {} -> ok, {} places ({}ms)", logLabel, places.size(), elapsedMillis(start));
			return places;
		} catch (HttpClientErrorException e) {
			// 401/403은 서버 키 설정 문제, 429는 쿼터 초과다. 어느 쪽이든 사용자가 고칠 수 없으므로 502로 돌려준다.
			log.warn("kakao call {} -> {} ({}ms)", logLabel, e.getStatusCode().value(), elapsedMillis(start));
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		} catch (RestClientException | IllegalArgumentException e) {
			log.warn("kakao call {} -> failed ({}ms): {}", logLabel, elapsedMillis(start), e.getMessage());
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		}
	}

	// 1.0E-5 같은 지수 표기가 쿼리에 들어가지 않게 한다.
	private static String plain(double value) {
		return BigDecimal.valueOf(value).toPlainString();
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SearchResponse(List<Document> documents) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Document(
		String id,
		@JsonProperty("place_name") String placeName,
		@JsonProperty("category_name") String categoryName,
		@JsonProperty("category_group_code") String categoryGroupCode,
		@JsonProperty("address_name") String addressName,
		@JsonProperty("road_address_name") String roadAddressName,
		String x,
		String y) {
	}
}
