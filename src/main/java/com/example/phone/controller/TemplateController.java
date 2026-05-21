package com.example.phone.controller;

import com.example.phone.service.TtsTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TtsTemplateService ttsTemplateService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", ttsTemplateService.findAll());
        return "templates";
    }

    @PostMapping("/add")
    public String add(@RequestParam String templateCode,
                      @RequestParam(required = false) String name,
                      RedirectAttributes ra) {
        try {
            ttsTemplateService.add(templateCode.trim(), name);
            ra.addFlashAttribute("msg", "添加成功: " + templateCode);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/templates";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            ttsTemplateService.toggle(id);
            ra.addFlashAttribute("msg", "状态已切换");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/templates";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            ttsTemplateService.delete(id);
            ra.addFlashAttribute("msg", "已删除");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "删除失败: " + e.getMessage());
        }
        return "redirect:/templates";
    }
}
