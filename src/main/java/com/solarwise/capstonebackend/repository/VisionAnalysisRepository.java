package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.VisionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisionAnalysisRepository extends JpaRepository<VisionAnalysis, Long> {
    // 요구사항 해결: Anomaly ID로 VisionAnalysis를 안전하게 조회하는 메소드 추가
    Optional<VisionAnalysis> findByAnomalyId(Long anomalyId);
}
