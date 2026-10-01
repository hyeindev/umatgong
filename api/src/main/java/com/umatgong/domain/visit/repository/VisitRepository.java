package com.umatgong.domain.visit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umatgong.domain.visit.entity.Visit;

public interface VisitRepository extends JpaRepository<Visit, Long> {
}
