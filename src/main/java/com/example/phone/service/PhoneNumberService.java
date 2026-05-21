package com.example.phone.service;

import com.example.phone.entity.PhoneNumber;
import com.example.phone.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhoneNumberService {

    private final PhoneNumberRepository repository;

    public List<PhoneNumber> findAll() {
        return repository.findAll();
    }

    public List<PhoneNumber> findEnabled() {
        return repository.findByEnabledTrue();
    }

    public List<String> findEnabledNumbers() {
        return repository.findByEnabledTrue().stream()
                .map(PhoneNumber::getNumber)
                .toList();
    }

    public PhoneNumber add(String number) {
        if (number == null || !number.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确: " + number);
        }
        repository.findByNumber(number).ifPresent(p -> {
            throw new IllegalArgumentException("号码已存在: " + number);
        });
        PhoneNumber phone = new PhoneNumber();
        phone.setNumber(number);
        phone.setEnabled(true);
        return repository.save(phone);
    }

    public void toggle(Long id) {
        PhoneNumber phone = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("号码不存在"));
        phone.setEnabled(!phone.isEnabled());
        repository.save(phone);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }
}
