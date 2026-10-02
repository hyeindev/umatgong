package com.umatgong.domain.visit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.visit.entity.VisitPhoto;

public interface VisitPhotoRepository extends JpaRepository<VisitPhoto, Long> {

	@Query("select p.thumbUrl from VisitPhoto p where p.visit.id = :visitId")
	List<String> findThumbUrlsByVisitId(@Param("visitId") Long visitId);
}
