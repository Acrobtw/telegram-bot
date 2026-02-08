package com.nick.bot;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.nick.bot.commands.BotCommands;
import com.nick.bot.service.YoutubeService;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;


@Component
public class MyBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final YoutubeService youtubeService;


    public MyBot(@Value("${bot.token}") String botToken, YoutubeService youtubeService) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.youtubeService = youtubeService;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();

        switch(text) {
            case "/start":
                sendMessage(chatId, String.format(BotCommands.START_TEMPLATE, firstName));
                return;
            case "/help":
                sendMessage(chatId, String.format(BotCommands.HELP_TEMPLATE, firstName));
                return;
        }

        if(isYoutubeLink(text)) {
            new Thread(() -> processAudioRequest(chatId, text)).start();
        } else {
            sendMessage(chatId, "Пришли мне ссылку на YouTube или нажми /help");
        }
    }



    private void processAudioRequest(long chatId, String url) {
        sendMessage(chatId, "⏳ Работаю над твоим треком...");
        File mp3File = youtubeService.downloadAudio(url, chatId);

        if (mp3File != null && mp3File.exists()) {
            sendMp3(chatId, mp3File);
            mp3File.delete();
        } else {
            sendMessage(chatId, BotCommands.ERROR_DOWNLOAD);
        }
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

    private boolean isYoutubeLink(String text) {
        return text.contains("youtube.com") || text.contains("youte.be");
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .parseMode("Markdown")
            .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
