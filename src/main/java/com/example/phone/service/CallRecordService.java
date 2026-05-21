package com.example.phone.service;

import com.example.phone.entity.CallRecord;
import com.example.phone.repository.CallRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallRecordService {

    private final CallRecordRepository repository;

    public CallRecord save(CallRecord record) {
        return repository.save(record);
    }

    public Page<CallRecord> findRecent(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<CallRecord> searchByAlertname(String alertname, int page, int size) {
        return repository.findByAlertnameContainingOrderByCreatedAtDesc(alertname, PageRequest.of(page, size));
    }

    public long count() {
        return repository.count();
    }
}
