package com.example.phone.controller;

import com.example.phone.service.PhoneNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneNumberService phoneNumberService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("phones", phoneNumberService.findAll());
        return "phones";
    }

    @PostMapping("/add")
    public String add(@RequestParam String number, RedirectAttributes ra) {
        try {
            phoneNumberService.add(number.trim());
            ra.addFlashAttribute("msg", "添加成功: " + number);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/phones";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        phoneNumberService.toggle(id);
        ra.addFlashAttribute("msg", "状态已切换");
        return "redirect:/phones";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        phoneNumberService.delete(id);
        ra.addFlashAttribute("msg", "已删除");
        return "redirect:/phones";
    }
}
