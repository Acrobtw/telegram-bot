package com.nick.bot.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class YoutubeService {
    
    @Value("${ffmpeg.path}")
    private String ffmpegPath;
    @Value("${ytdlp.path}")
    private String ytdlpPath;


    public File downloadAudio(String url, long chatId) {
        try {
            new File("downloads").mkdirs();
            String outputTemplate = "downloads/" + chatId + "_%(title).100s.%(ext)s";


            ProcessBuilder pb = new ProcessBuilder(
                ytdlpPath,
                "--ffmpeg-location", ffmpegPath,
                "--no-playlist", "-x", "--audio-format", "mp3",
                "--audio-quality", "192K", "--no-part",
                "-o", outputTemplate, url
            );
            
            pb.inheritIO();
            if(pb.start().waitFor() == 0) {
                File dir = new File("downloads");
                File files[] = dir.listFiles((d, name) -> name.startsWith(chatId + "_") && name.endsWith(".mp3"));
                return (files != null && files.length > 0) ? files[0] : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
