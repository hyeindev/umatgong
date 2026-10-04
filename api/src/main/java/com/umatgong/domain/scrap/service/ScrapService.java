package com.umatgong.domain.scrap.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umatgong.domain.club.repository.ClubMemberRepository;
import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.place.repository.PlaceRepository;
import com.umatgong.domain.scrap.dto.ScrapResponse;
import com.umatgong.domain.scrap.dto.ScrapStatusResponse;
import com.umatgong.domain.scrap.repository.ScrapRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 스크랩 (가고 싶은 곳). 나만 보는 목록이다.
 *
 * <p><b>클럽 경계:</b> 카카오 장소는 누구나 스크랩할 수 있다 (장소 자체는 공개 정보다).
 * 클럽 전용 장소는 그 클럽의 <b>지금</b> 멤버만 다룰 수 있고, 아니면 없는 장소와 같은 PLACE_NOT_FOUND다.
 * 클럽을 나가면 그 클럽 전용 장소의 스크랩은 목록·상태에서 빠진다 (지우지는 않는다. 다시 들어오면 보인다).
 */
@Service
@RequiredArgsConstructor
public class ScrapService {

	static final int MAX_LIST = 200;

	private final ScrapRepository scrapRepository;
	private final PlaceRepository placeRepository;
	private final ClubMemberRepository clubMemberRepository;

	@Transactional
	public ScrapStatusResponse add(Long userId, Long placeId) {
		requireAccessible(userId, placeId);
		scrapRepository.add(userId, placeId);
		return new ScrapStatusResponse(placeId, true);
	}

	@Transactional
	public ScrapStatusResponse remove(Long userId, Long placeId) {
		requireAccessible(userId, placeId);
		scrapRepository.remove(userId, placeId);
		return new ScrapStatusResponse(placeId, false);
	}

	@Transactional(readOnly = true)
	public ScrapStatusResponse status(Long userId, Long placeId) {
		requireAccessible(userId, placeId);
		return new ScrapStatusResponse(placeId, scrapRepository.exists(userId, placeId));
	}

	@Transactional(readOnly = true)
	public List<ScrapResponse> mine(Long userId) {
		List<ScrapRepository.Row> rows = scrapRepository.mine(userId,
			clubMemberRepository.findClubIdsByUserId(userId), MAX_LIST);
		if (rows.isEmpty()) {
			return List.of();
		}
		Map<Long, Place> places = placeRepository.findAllById(rows.stream().map(ScrapRepository.Row::placeId).toList())
			.stream()
			.collect(Collectors.toMap(Place::getId, Function.identity()));
		return rows.stream()
			.filter(row -> places.containsKey(row.placeId()))
			.map(row -> new ScrapResponse(PlaceResponse.of(places.get(row.placeId()), null), row.scrappedAt()))
			.toList();
	}

	private void requireAccessible(Long userId, Long placeId) {
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
		if (place.isCustom()
			&& !clubMemberRepository.findClubIdsByUserId(userId).contains(place.getClub().getId())) {
			throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
		}
	}
}
