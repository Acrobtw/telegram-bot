package com.nick.bot;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;


@Component
public class MyBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final String botUsername;


    public MyBot(@Value("${bot.token}") String botToken, @Value("${bot.name}") String botUsername) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public void consume(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String url = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();


            if(url.contains("youtube.com") || url.contains("youtu.be")) {
               new Thread(() -> {
                sendMessage(chatId, "⏳ Начинаю работу над твоим треком...");
                File mp3File = downloadAudio(url, chatId);
                if(mp3File != null && mp3File.exists()) {
                    sendMp3(chatId, mp3File);
                    mp3File.delete();
                } else {
                    sendMessage(chatId, "❌ Ошибка скачивания.");
                }
               }).start();
            }
        }
    }



    private File downloadAudio(String url, long chatId) {
        try  {

            new File("downloads").mkdirs();

            String outputTemplate = "downloads/" + chatId + "_%(title).100s.%(ext)s";

            ProcessBuilder pb = new ProcessBuilder(
                "D:\\Downloads\\yt-dlp.exe",
                "--ffmpeg-location", "D:\\Downloads\\ffmpeg-2026-01-29-git-c898ddb8fe-full_build\\ffmpeg\\bin",
                "--no-playlist",
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "192K",
                "--no-part",
                "-o", outputTemplate,
                url
            );

            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if(exitCode == 0) {
                File dir = new File("downloads");
                File[] files = dir.listFiles((d, name) -> name.startsWith(chatId + "_") && name.endsWith(".mp3"));

                if(files != null && files.length > 0) {
                    return files[0];
                }
            }

        } catch (Exception e) 
        {
            e.printStackTrace();
        }
        return null;
    }

    private void sendMp3(long chatId, File file) {
        String cleanName = file.getName()
                .replaceFirst("^\\d+_", "")
                .replace(".mp3", " ");

        SendAudio audio = SendAudio.builder()
        .chatId(chatId)
        .audio(new InputFile(file))
        .title(cleanName)
        .build();

        try {
            telegramClient.execute(audio);
            System.out.println("Файл успешно отправлен!");
        } catch (TelegramApiException e) { 
            System.err.println("Ошибка отправки " + e.getMessage());
            sendMessage(chatId, "❌ Файл слишком большой для отправки (больше 50 МБ)");
         }
    }


    private void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
