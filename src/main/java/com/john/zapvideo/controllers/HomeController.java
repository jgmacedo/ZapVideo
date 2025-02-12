package com.john.zapvideo.controllers;

import com.john.zapvideo.services.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final String BASE_DIR = "/root/downloads";

    // Verifica se o caminho do arquivo é válido
    private boolean isValidFilePath(String filePath) {
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        return absolutePath.startsWith(BASE_DIR);
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
    public ResponseEntity<Resource> downloadFile(@RequestParam("id") String videoId) {
        try {
            String filePath = VideoService.getFilePathById(videoId);
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException("File not found");
            }

            Resource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
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

        return "redirect:/download.html?id=" + id; // Redirect to the static HTML page with the filePath and video ID
    }

    @DeleteMapping("/deleteVideo")
    public ResponseEntity<String> deleteVideo(@RequestParam String videoId) {
        try {
            VideoService.deleteVideoById(videoId);
            return ResponseEntity.ok("Video deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete video: " + e.getMessage());
        }
    }
}
