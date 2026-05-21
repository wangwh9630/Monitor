package com.example.phone.repository;

import com.example.phone.entity.CallRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    Page<CallRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CallRecord> findByAlertnameContainingOrderByCreatedAtDesc(String alertname, Pageable pageable);
}
