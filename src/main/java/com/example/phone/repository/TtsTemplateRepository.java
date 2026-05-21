package com.example.phone.repository;

import com.example.phone.entity.TtsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TtsTemplateRepository extends JpaRepository<TtsTemplate, Long> {

    List<TtsTemplate> findByEnabledTrue();

    Optional<TtsTemplate> findFirstByEnabledTrue();
}
