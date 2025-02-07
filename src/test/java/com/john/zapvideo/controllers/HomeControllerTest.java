package com.john.zapvideo.controllers;

import com.john.zapvideo.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @Mock
    private Model model;

    @InjectMocks
    private HomeController homeController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessVideo() throws Exception {
        when(videoService.processVideo(anyString())).thenReturn("12345");

        mockMvc.perform(post("/process")
                        .param("url", "http://example.com/video.mp4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/downloadPage?id=12345"));

        verify(videoService, times(1)).processVideo(anyString());
    }

    @Test
    public void testDownloadFile() throws Exception {
        when(videoService.getFilePathById("12345")).thenReturn("/root/downloads/video_12345.mp4");

        mockMvc.perform(get("/download")
                        .param("id", "12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"video_12345.mp4\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));

        verify(videoService, times(1)).getFilePathById("12345");
    }

    @Test
    public void testDeleteVideo() throws Exception {
        doNothing().when(videoService).deleteVideoById("12345");

        mockMvc.perform(delete("/deleteVideo")
                        .param("videoId", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("Video deleted successfully"));

        verify(videoService, times(1)).deleteVideoById("12345");
    }
}