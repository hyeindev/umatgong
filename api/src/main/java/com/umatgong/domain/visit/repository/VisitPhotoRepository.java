package com.umatgong.domain.visit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umatgong.domain.visit.entity.VisitPhoto;

public interface VisitPhotoRepository extends JpaRepository<VisitPhoto, Long> {
}
