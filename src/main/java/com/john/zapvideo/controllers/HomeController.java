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

    // Envia a requisição de download/processamento do vídeo
    @PostMapping("/process")
    public String processVideo(@RequestParam String url, Model model) {
        String id = null;
        String filePath = null;
        try {
            // Process the video and get its ID
            id = VideoService.processVideo(url);
            // Get the file path using the ID
            filePath = VideoService.getFilePathById(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // If the file path exists, redirect to the download page
        if (filePath != null) {
            // URL-encode the file path for safe use in URL
            String encodedFilePath = null;
            try {
                encodedFilePath = URLEncoder.encode(filePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            // Redirect to the /download endpoint with the encoded file path
            return "redirect:/download?filePath=" + encodedFilePath;
        } else {
            model.addAttribute("error", "Video processing failed");
            return "error"; // Show an error page if processing fails
        }
    }


    // Serve o arquivo para download
    @GetMapping("/download")
    @ResponseBody
    public void downloadFile(@RequestParam String filePath, HttpServletResponse response) throws IOException {
        // Cria o objeto File com o caminho fornecido
        File file = new File(filePath);

        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Define os headers da resposta para o download do arquivo
        response.setContentType(Files.probeContentType(file.toPath()));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());

        // Stream o conteúdo do arquivo para a resposta
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
    }


    // Redireciona para a página de download
    @GetMapping("/redirect")
    public String redirectToDownloadPage(@RequestParam String id, Model model) {
        String filePath = VideoService.getFilePathById(id);

        if (filePath == null) {
            model.addAttribute("error", "Vídeo não encontrado");
            return "error"; // Exibe uma página de erro caso o vídeo não seja encontrado
        }

        // Codifica o caminho do arquivo para a URL
        String encodedFilePath = null;
        try {
            encodedFilePath = URLEncoder.encode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        // Redireciona para a URL de download com o caminho do arquivo codificado
        return "redirect:/download?filePath=" + encodedFilePath;
    }
}
