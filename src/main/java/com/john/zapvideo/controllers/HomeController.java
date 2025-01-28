package com.john.zapvideo.controllers;

import com.john.zapvideo.services.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/")
public class HomeController {

    // Verifica se o caminho do arquivo é válido
    private boolean isValidFilePath(String filePath) {
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        String baseDir = "/path/to/your/uploads";  // Defina o diretório base para os vídeos

        return absolutePath.startsWith(baseDir);
    }

    // Envia a requisição de download/processamento do vídeo
    @PostMapping("/process")
    public String processVideo(@RequestParam String url, Model model) {
        String id = null;
        try {
            id = VideoService.processVideo(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (id != null) {
            return "redirect:/downloadPage?id=" + id;
        } else {
            model.addAttribute("error", "Video processing failed");
            return "error"; // Display an error page if processing fails
        }
    }

    // Serve o arquivo para download
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        Path path = Paths.get(filePath);
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error in file path", e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }

    // Redireciona para a página de download
    @GetMapping("/redirect")
    public String redirectToDownloadPage(@RequestParam String id, Model model) {
        String filePath = VideoService.getFilePathById(id);

        if (filePath == null) {
            model.addAttribute("error", "Vídeo não encontrado");
            return "error"; // Exibe uma página de erro caso o vídeo não seja encontrado
        }

        String encodedFilePath = null;
        try {
            encodedFilePath = URLEncoder.encode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return "redirect:/download?filePath=" + encodedFilePath;
    }

    @GetMapping("/downloadPage")
    public String downloadPage(@RequestParam String id, Model model) {
        String filePath = VideoService.getFilePathById(id);

        if (filePath == null) {
            model.addAttribute("error", "Video not found");
            return "error"; // Display an error page if the video is not found
        }

        String encodedFilePath = null;
        try {
            encodedFilePath = URLEncoder.encode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        System.out.println(encodedFilePath);
        System.out.println(id);

        return "redirect:/download.html?filePath=" + encodedFilePath + "&id=" + id; // Redirect to the static HTML page with the filePath and video ID
    }

    private String extractVideoIdFromFilePath(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null) {
            return parentDir.getName();
        }
        return null;
    }
}
