package com.umatgong.domain.place.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findAllByExternalIdIn(Collection<String> externalIds);

	/**
	 * 반경 안의 클럽 전용 장소. clubIds는 요청자가 지금 속한 클럽만 넘긴다 (빈 목록은 넘기지 않는다).
	 * ST_DWithin(geography)은 GIST 인덱스를 탄다. ST_MakePoint(경도, 위도).
	 */
	@Query(value = """
		select * from places p
		where p.source = 'USER' and p.club_id in (:clubIds)
		  and ST_DWithin(p.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
		""", nativeQuery = true)
	List<Place> findCustomWithin(@Param("clubIds") Collection<Long> clubIds, @Param("lat") double lat,
		@Param("lng") double lng, @Param("radius") double radiusMeters);

	/** 이름에 검색어가 들어간 클럽 전용 장소. pattern은 호출하는 쪽이 %·_를 이스케이프해 넘긴다 */
	@Query(value = """
		select * from places p
		where p.source = 'USER' and p.club_id in (:clubIds) and p.name ilike :pattern escape '\\'
		order by p.name, p.id
		limit :limit
		""", nativeQuery = true)
	List<Place> findCustomByName(@Param("clubIds") Collection<Long> clubIds, @Param("pattern") String pattern,
		@Param("limit") int limit);
}
