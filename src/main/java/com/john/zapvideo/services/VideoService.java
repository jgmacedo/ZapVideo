package com.john.zapvideo.services;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class VideoService {

    private static final String DOWNLOAD_DIR = "root/downloads"; // Base directory for storing videos
    private static final Map<String, String> videoPaths = new ConcurrentHashMap<>(); // Map to store video paths by ID
    private static final int MAX_THREADS = 4; // Maximum number of threads for processing videos
    private static final int DOWNLOAD_TIMEOUT_MINUTES = 5; // Timeout for download in minutes

    /**
     * Processes multiple video downloads asynchronously.
     *
     * @param urls List of URLs to download.
     * @return A list of paths to the downloaded videos.
     */
    public static List<String> processMultipleVideos(List<String> urls) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String url : urls) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> processVideo(url), executorService);
            futures.add(future);
        }

        List<String> downloadedVideos = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                downloadedVideos.add(future.get()); // Get the result of each download
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                downloadedVideos.add("Error: " + e.getMessage());
            }
        }
        executorService.shutdown();
        return downloadedVideos;
    } // unused

    /**
     * Processes a video download from the given URL.
     *
     * @param url The URL of the video to download.
     * @return The unique ID of the downloaded video.
     */
    public static String processVideo(String url) {
        validateUrl(url);

        File baseDir = initializeBaseDirectory();
        File uniqueDir = createUniqueDirectory(baseDir);

        ProcessBuilder processBuilder = buildProcess(url, uniqueDir);
        System.out.println("Executing command: " + String.join(" ", processBuilder.command()));

        return executeDownload(processBuilder, uniqueDir);
    }

    /**
     * Validates the provided URL.
     *
     * @param url The URL to validate.
     */
    private static void validateUrl(String url) {
        if (url == null || url.isEmpty() || !url.startsWith("http")) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        // Verificar se a URL contém o domínio de vídeos
        if (!url.contains("youtube") && !url.contains("vimeo")) {
            throw new IllegalArgumentException("URL não é suportada: " + url);
        }
    }

    /**
     * Ensures the base download directory exists.
     *
     * @return The base download directory as a File object.
     */
    private static File initializeBaseDirectory() {
        File baseDir = new File(DOWNLOAD_DIR);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new RuntimeException("Failed to create base directory: " + baseDir.getAbsolutePath());
        }
        if (!baseDir.isDirectory()) {
            throw new RuntimeException("Expected a directory but found: " + baseDir.getAbsolutePath());
        }
        System.out.println("Base directory initialized: " + baseDir.getAbsolutePath());
        return baseDir;
    }

    /**
     * Creates a unique subdirectory for storing the downloaded video.
     *
     * @param baseDir The base directory where the unique directory will be created.
     * @return The unique directory as a File object.
     */
    private static File createUniqueDirectory(File baseDir) {
        String uniqueDirPath = baseDir + File.separator + UUID.randomUUID().toString();
        File uniqueDir = new File(uniqueDirPath);
        if (!uniqueDir.exists() && !uniqueDir.mkdirs()) {
            throw new RuntimeException("Failed to create unique directory: " + uniqueDirPath);
        }
        System.out.println("Unique directory created: " + uniqueDir.getAbsolutePath());
        return uniqueDir;
    }

    /**
     * Builds the ProcessBuilder for executing the yt-dlp command.
     *
     * @param url       The video URL.
     * @param uniqueDir The directory to store the downloaded video.
     * @return The configured ProcessBuilder.
     */
    private static ProcessBuilder buildProcess(String url, File uniqueDir) {
        return new ProcessBuilder(
                "yt-dlp", // Replace with the absolute path if necessary
                "-f", "mp4",
                "-o", uniqueDir.getAbsolutePath() + File.separator + "%(title)s.%(ext)s",
                url
        );
    }

    /**
     * Executes the download process and retrieves the downloaded video file.
     *
     * @param processBuilder The ProcessBuilder for the download process.
     * @param uniqueDir      The directory where the video is stored.
     * @return The unique ID for the downloaded video.
     */
    private static String executeDownload(ProcessBuilder processBuilder, File uniqueDir) {
        try {
            Process process = processBuilder.start();
            consumeStream(process);

            if (!process.waitFor(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new TimeoutException("Download process timed out.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Download failed with exit code: " + exitCode);
            }

            String filePath = findDownloadedFile(uniqueDir);
            String videoId = UUID.randomUUID().toString(); // Generate a unique ID for this video

            // Store the video file path in the map with the ID as the key
            videoPaths.put(videoId, filePath);

            return videoId; // Return the unique ID for this video
        } catch (IOException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            return "Error processing video: " + e.getMessage();
        }
    }

    /**
     * Reads and returns the downloaded video file from the unique directory.
     *
     * @param uniqueDir The directory containing the downloaded video.
     * @return The absolute path of the downloaded video file.
     * @throws IOException If no file or multiple files are found.
     */
    private static String findDownloadedFile(File uniqueDir) throws IOException {
        // List files that match common video formats
        File[] files = uniqueDir.listFiles((dir, name) -> name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm"));

        if (files == null || files.length == 0) {
            throw new IOException("No video files found.");
        }

        if (files.length > 1) {
            System.out.println("Warning: Multiple video files found, selecting the first one.");
        }

        File videoFile = files[0]; // Escolher o primeiro arquivo encontrado

        String newFileName = videoFile.getName().replace(" ", "_"); // Renomear para evitar espaços
        if (!newFileName.equals(videoFile.getName())) {
            File renamedFile = new File(videoFile.getParent(), newFileName);
            if (!videoFile.renameTo(renamedFile)) {
                throw new IOException("Failed to rename the file.");
            }
            videoFile = renamedFile;
        }

        return videoFile.getAbsolutePath(); // Retorna o caminho do arquivo renomeado
    }

    /**
     * Consumes the process output streams to avoid deadlocks.
     *
     * @param process The process whose streams need to be consumed.
     */
    private static void consumeStream(Process process) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Retrieves the file path of a video by its unique ID.
     *
     * @param id The unique ID of the video.
     * @return The file path of the video, or null if not found.
     */
    public static String getFilePathById(String id) {
        return videoPaths.get(id); // Return the file path associated with the ID
    }

    /**
     * Deletes a video file by its unique ID.
     * @param videoId The unique ID of the video to delete.
     */
    public static void deleteVideoById(String videoId) {
        String filePath = videoPaths.get(videoId);
        if (filePath != null) {
            File videoFile = new File(filePath);
            if (videoFile.exists()) {
                if (videoFile.delete()) {
                    System.out.println("Successfully deleted video: " + filePath);
                } else {
                    System.err.println("Failed to delete video: " + filePath);
                }
            } else {
                System.err.println("Video file not found: " + filePath);
            }
        } else {
            System.err.println("No video found with ID: " + videoId);
        }
    }
}
