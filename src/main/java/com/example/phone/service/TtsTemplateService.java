package com.example.phone.service;

import com.example.phone.entity.TtsTemplate;
import com.example.phone.repository.TtsTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TtsTemplateService {

    private final TtsTemplateRepository repository;

    public List<TtsTemplate> findAll() {
        return repository.findAll();
    }

    public List<TtsTemplate> findEnabled() {
        return repository.findByEnabledTrue();
    }

    public String getActiveTemplateCode() {
        return repository.findFirstByEnabledTrue()
                .map(TtsTemplate::getTemplateCode)
                .orElse(null);
    }

    public TtsTemplate add(String templateCode, String name) {
        TtsTemplate template = new TtsTemplate();
        template.setTemplateCode(templateCode);
        template.setName(name);
        template.setEnabled(true);
        return repository.save(template);
    }

    public void toggle(Long id) {
        TtsTemplate template = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        template.setEnabled(!template.isEnabled());
        repository.save(template);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }
}
