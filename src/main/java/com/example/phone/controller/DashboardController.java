package com.example.phone.controller;

import com.example.phone.service.CallRecordService;
import com.example.phone.service.PhoneNumberService;
import com.example.phone.service.TtsTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CallRecordService callRecordService;
    private final PhoneNumberService phoneNumberService;
    private final TtsTemplateService ttsTemplateService;

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String alertname,
                            @PageableDefault(size = 20) Pageable pageable,
                            Model model) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int page = pageable.getPageNumber();
        if (alertname != null && !alertname.isBlank()) {
            model.addAttribute("records", callRecordService.searchByAlertname(alertname, page, size));
        } else {
            model.addAttribute("records", callRecordService.findRecent(page, size));
        }
        model.addAttribute("phoneCount", phoneNumberService.count());
        model.addAttribute("callCount", callRecordService.count());
        model.addAttribute("templateCount", ttsTemplateService.count());
        model.addAttribute("alertname", alertname);
        return "dashboard";
    }
}
