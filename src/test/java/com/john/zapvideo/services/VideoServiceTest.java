package com.john.zapvideo.services;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VideoServiceTest {

    @Test
    void testProcessVideo() {
        String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        String videoId = VideoService.processVideo(videoUrl);

        assertNotNull(videoId, "The video ID should not be null");
        assertTrue(videoId.matches("^[a-f0-9\\-]{36}$"), "The video ID should be a valid UUID");
    }

    @Test
    void testGetFilePathById() {
        String videoId = UUID.randomUUID().toString();
        String filePath = "/root/downloads/" + videoId + "/video.mp4";

        // Simulate adding a video path to the map
        VideoService.addVideoPath(videoId, filePath);

        // Retrieve the file path using the ID
        String retrievedFilePath = VideoService.getFilePathById(videoId);

        // Assert that the retrieved file path matches the expected file path
        assertEquals(filePath, retrievedFilePath, "The retrieved file path should match the expected file path");
    }

    @Test
    void deleteVideoById() {
    }

    @Test
    void testDeleteVideoById() {
    }
}