package com.john.zapvideo.services;

import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class VideoService {

    private static final String DOWNLOAD_DIR = "downloads"; // Diretório onde os vídeos serão armazenados

    public static String processVideo(String url) {
        // Cria uma pasta única baseada em um UUID
        String uniqueDir = DOWNLOAD_DIR + File.separator + UUID.randomUUID().toString();
        File downloadDir = new File(uniqueDir);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs(); // Cria o diretório se não existir
        }

        // Monta o comando para o yt-dlp
        String command = "yt-dlp -o \"" + downloadDir + File.separator + "%(title)s.%(ext)s\" " + "\""+url+"\"";
        System.out.println(command);
        try {
            // Executa o yt-dlp no diretório temporário
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.directory(downloadDir);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Verifica se o processo foi bem-sucedido
            if (exitCode != 0) {
                throw new IOException("Erro no download. Código de saída: " + exitCode);
            }

            // Verifica se há apenas um arquivo na pasta
            File[] files = downloadDir.listFiles();
            if (files != null && files.length == 1) {
                // Retorna o caminho do arquivo
                return files[0].getAbsolutePath();
            } else {
                throw new IOException("Erro: Mais de um arquivo encontrado ou nenhum arquivo gerado.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Erro ao processar o vídeo: " + e.getMessage();
        }
    }
}
