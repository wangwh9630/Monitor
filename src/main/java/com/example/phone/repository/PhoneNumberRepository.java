package com.example.phone.repository;

import com.example.phone.entity.PhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {

    List<PhoneNumber> findByEnabledTrue();

    Optional<PhoneNumber> findByNumber(String number);
}
