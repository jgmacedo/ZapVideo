package com.john.zapvideo.controllers;
import com.john.zapvideo.services.VideoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping("/")
public class HomeController {

    // envia a requisição de download/processamento do vídeo
    @PostMapping("/process")
    public String processVideo(@RequestParam String url,Model model) {
        String id = null;
        try {
            id = VideoService.processVideo(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("id", id);
        return "redirect:/process/" + id;
    }

    @GetMapping("/download")
    @ResponseBody
    public byte[] downloadFile(@RequestParam String filePath) throws IOException {
        File file = new File(filePath);
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/processing")
    public String processingVideo(@RequestParam String url, Model model) {
        return "processing";
    }
}
