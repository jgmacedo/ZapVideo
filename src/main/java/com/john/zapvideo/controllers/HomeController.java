package com.john.zapvideo.controllers;

import com.john.zapvideo.services.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;


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
        String filePath = null;
        try {
            // Processa o vídeo e obtém o ID
            id = VideoService.processVideo(url);
            // Obtém o caminho do arquivo usando o ID
            filePath = VideoService.getFilePathById(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Se o caminho do arquivo existir, redireciona para a página de download
        if (filePath != null) {
            String encodedFilePath = null;
            try {
                encodedFilePath = URLEncoder.encode(filePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return "redirect:/download?filePath=" + encodedFilePath;
        } else {
            model.addAttribute("error", "Video processing failed");
            return "error"; // Exibe uma página de erro caso o processamento falhe
        }
    }

    // Serve o arquivo para download
    @GetMapping("/download")
    @ResponseBody
    public void downloadFile(@RequestParam String filePath, HttpServletResponse response) throws IOException {
        if (!isValidFilePath(filePath)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(Files.probeContentType(file.toPath()));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
             BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream())) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo", e);
        }

        String videoId = extractVideoIdFromFilePath(filePath);
        VideoService.deleteVideoById(videoId); // Deleta o vídeo após o download
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

    private String extractVideoIdFromFilePath(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null) {
            return parentDir.getName();
        }
        return null;
    }
}
