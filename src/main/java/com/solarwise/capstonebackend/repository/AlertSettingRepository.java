package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.AlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertSettingRepository extends JpaRepository<AlertSetting, Long> {
    Optional<AlertSetting> findByUserId(Long userId);
}
