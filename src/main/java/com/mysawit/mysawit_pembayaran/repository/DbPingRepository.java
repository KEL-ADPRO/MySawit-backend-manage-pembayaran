package com.mysawit.mysawit_pembayaran.repository;

import com.mysawit.mysawit_pembayaran.model.DbPing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbPingRepository extends JpaRepository<DbPing, Long> {
}