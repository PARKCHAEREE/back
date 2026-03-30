package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.PowerPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 발전소 저장소
 */
@Repository
public interface PowerPlantRepository extends JpaRepository<PowerPlant, Long> {

    List<PowerPlant> findByUserId(Long userId);

    Optional<PowerPlant> findByIdAndUserId(Long id, Long userId);

}

