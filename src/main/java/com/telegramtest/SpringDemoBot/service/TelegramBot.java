package com.telegramtest.SpringDemoBot.service;

import com.telegramtest.SpringDemoBot.config.BotConfig;
import com.telegramtest.SpringDemoBot.model.User;
import com.telegramtest.SpringDemoBot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot  {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;
    final String helpText = EmojiParser.parseToUnicode("This bot is create for education purpose :snowflake:\n" +
            "Type /start to get welcome message \n" +
            "Type /help to see this message again");

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data info"));
        listOfCommands.add(new BotCommand("/deletedata", "delete your data info"));
        listOfCommands.add(new BotCommand("/help", "get instruction how to use bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/register", "register your user"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot's commands: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatId, helpText);
                    break;
                case "/register":
                    register(chatId);
                    break;
                default:
                    sendMessage(chatId, "Command not found");
                    break;
            }
        }
        else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals("YES_BUTTON")) {
                String text = "You pressed Yes button";
                EditMessageText editedMessageText = new EditMessageText();
                editedMessageText.setChatId(chatId);
                editedMessageText.setText(text);
                editedMessageText.setMessageId(messageId);
                try {
                    execute(editedMessageText);
                }
                catch (TelegramApiException e) {
                    log.error("Error: " + e.getMessage());
                }

            }
            else if (callBackData.equals("NO_BUTTON")) {
                String text = "You pressed No button";
                EditMessageText editedMessageText = new EditMessageText();
                editedMessageText.setChatId(chatId);
                editedMessageText.setText(text);
                editedMessageText.setMessageId(messageId);

                try {
                    execute(editedMessageText);
                }
                catch (TelegramApiException e) {
                    log.error("Error: " + e.getMessage());
                }
            }

        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            long chatId = msg.getChatId();
            Chat chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    public void startCommandReceived(long chatId, String name) {
        String answear = EmojiParser.parseToUnicode("Hi " + name + ", it's a test bot :santa:");
        log.info("Replied to user " + name);

        sendMessage(chatId, answear);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>(); //список всех рядов с кнопками

        KeyboardRow row = new KeyboardRow(); //кнопки в строке

        row.add("Days left to christmas"); //добавление кнопок в строку
        row.add("Get random number");

        keyboardRows.add(row); //добавление всего ряда

        row = new KeyboardRow(); // новый ряд

        row.add("Get random phrases");
        row.add("Check my data");
        row.add("Delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    private void register(Long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do your really want to register?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup(); //Встроенная разметка клавиатуры
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>(); //Список строк кнопок под текстом
        List<InlineKeyboardButton> rowInline = new ArrayList<>(); //строка кнопок
        InlineKeyboardButton yesButton = new InlineKeyboardButton(); //кнопка

        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON"); //если пользователь нажмет Yes, то значение callbackData будет YES_BUTTON
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");

        rowInline.add(yesButton); //доабавляется в строку кнопок
        rowInline.add(noButton);

        rowsInLine.add(rowInline);

        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline); // когда будет отправлено юзеру, то он получит кнопки под текстом

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }



}
