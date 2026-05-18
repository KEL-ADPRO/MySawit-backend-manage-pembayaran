package com.mysawit.pembayaran.repository;

import com.mysawit.pembayaran.model.WageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WageConfigRepository extends JpaRepository<WageConfig, UUID> {

    Optional<WageConfig> findTopByOrderByUpdatedAtDesc();
}
