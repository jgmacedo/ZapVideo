package com.john.zapvideo.services;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class VideoService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.home") + "/downloads"; // Base directory for storing videos
    private static final Map<String, String> videoPaths = new ConcurrentHashMap<>(); // Map to store video paths by ID
    private static final int MAX_THREADS = 4; // Maximum number of threads for processing videos
    private static final int DOWNLOAD_TIMEOUT_MINUTES = 5; // Timeout for download in minutes

    public static String processVideo(String url) {
        validateUrl(url);
        File baseDir = initializeBaseDirectory();
        UniqueDirectory uniqueDirectory = createUniqueDirectory(baseDir);
        File uniqueDir = uniqueDirectory.getDirectory();
        String videoId = uniqueDirectory.getUuid();

        ProcessBuilder processBuilder = buildProcess(url, uniqueDir);
        System.out.println("Executing command: " + String.join(" ", processBuilder.command()));
        String downloadedVideoId = executeDownloadToServer(processBuilder, uniqueDir, videoId);

        if (!downloadedVideoId.equals(videoId)) {
            throw new RuntimeException("Mismatch between created UUID and returned UUID");
        }

        System.out.println("Video ID: " + videoId);
        return videoId;
    }

    private static void validateUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        if (!url.contains("youtube") && !url.contains("youtu.be")) {
            throw new IllegalArgumentException("URL not supported: " + url);
        }
    }

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

    private static UniqueDirectory createUniqueDirectory(File baseDir) {
        String uuid = UUID.randomUUID().toString();
        String uniqueDirPath = baseDir + File.separator + uuid;
        File uniqueDir = new File(uniqueDirPath);
        if (!uniqueDir.exists() && !uniqueDir.mkdirs()) {
            throw new RuntimeException("Failed to create unique directory: " + uniqueDirPath);
        }
        System.out.println("Unique directory created: " + uniqueDir.getAbsolutePath());
        return new UniqueDirectory(uniqueDir, uuid);
    }

    private static ProcessBuilder buildProcess(String url, File uniqueDir) {
        return new ProcessBuilder(
                "yt-dlp",
                "--cookies", "cookies.txt",
                "-f", "mp4",
                "-o", uniqueDir.getAbsolutePath() + File.separator + "%(title)s.%(ext)s",
                url
        );
    }

    private static String executeDownloadToServer(ProcessBuilder processBuilder, File uniqueDir, String videoId) {
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

            // Store the video file path in the map with the ID as the key
            videoPaths.put(videoId, filePath);

            System.out.println(videoId + " - Video downloaded: " + filePath);
            return videoId; // Return the unique ID for this video
        } catch (IOException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            return "Error processing video: " + e.getMessage();
        }
    }

    private static String findDownloadedFile(File uniqueDir) throws IOException {
        File[] files = uniqueDir.listFiles((dir, name) -> name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm"));

        if (files == null || files.length == 0) {
            throw new IOException("No video files found.");
        }

        if (files.length > 1) {
            System.out.println("Warning: Multiple video files found, selecting the first one.");
        }

        File videoFile = files[0];

        String newFileName = videoFile.getName().replace(" ", "_");
        if (!newFileName.equals(videoFile.getName())) {
            File renamedFile = new File(videoFile.getParent(), newFileName);
            if (!videoFile.renameTo(renamedFile)) {
                throw new IOException("Failed to rename the file.");
            }
            videoFile = renamedFile;
        }

        return videoFile.getAbsolutePath();
    }

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

    public static String getFilePathById(String id) {
        String filePath = videoPaths.get(id);
        System.out.println("Retrieving file path for ID: " + id + " -> " + filePath);
        return filePath;
    }

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

    public static void clearVideoPaths() {
        videoPaths.clear();
    }

    public static void addVideoPath(String id, String filePath) {
        videoPaths.put(id, filePath);
    }
}