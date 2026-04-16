package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.VisionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 이미지 분석 저장소
 */
@Repository
public interface VisionAnalysisRepository extends JpaRepository<VisionAnalysis, Long> {
}
