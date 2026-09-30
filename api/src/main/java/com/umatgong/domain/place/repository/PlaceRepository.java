package com.umatgong.domain.place.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umatgong.domain.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findAllByExternalIdIn(Collection<String> externalIds);
}
