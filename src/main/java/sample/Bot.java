package sample;

import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import javafx.scene.image.Image;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Audio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
//import org.telegram.telegrambots.meta.api.objects.File;
import java.io.File;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

import java.io.FileInputStream;
import java.io.IOException;

public class Bot extends TelegramLongPollingBot {
    //создаем две константы, присваиваем им значения токена и имя бота соответсвтенно
    //вместо звездочек подставляйте свои данные
    final private String BOT_TOKEN = "5401035487:AAEQlxp44vLeiMu9lHUI6dDw51xqBg8u9gE";
    final private String BOT_NAME = "TestIDEytevv_bot";
    private static final String GOOGLE_APPLICATION_CREDENTIALS = "credentials.json";
    ReplyKeyboardMarkup replyKeyboardMarkup;
    private String shop = "all";

    Excel excel = new Excel();
    Excel BARCODE = new Excel();

    Bot()
    {
        excel.createExcel("C:\\Users\\Cashless\\Desktop\\DataForTelegramBot\\Номенклатура_30.05.23.xlsx", 0);
        BARCODE.createExcel("C:\\Users\\Cashless\\Desktop\\DataForTelegramBot\\BARCODE.xlsx", 0);
        initKeyboard();
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try{

            if(update.getMessage().hasPhoto()){

                BarCodeRead barCodeRead = new BarCodeRead();
                BufferedImage imgB = null;
                //если не будет работать корректно(устанавливать соединение),
                // значит нужно делать запрос с fileId(https://api.telegram.org/bot<token>/getfile?file_id={the file_id of the photo you want to download}),
                // получать filepath, и уже его вставлять в форму https://api.telegram.org/file/bot<token>/<file_path> СДЕЛАНО!!!
                imgB = ImageIO.read(downloadImg("https://api.telegram.org/file/bot"+getBotToken() + "/" + getFilePath(update.getMessage().getPhoto().get(3).getFileId())));
                String barCodeStr = barCodeRead.getBarCode(imgB);

                Message inMess = update.getMessage();
                String chatId = inMess.getChatId().toString();
                //ищем сначало продукт по штрих коду в одной базе, потом в другой ищем цену и артикул по названию
                String response = findProduct(findProductFromBarCode(barCodeStr));

                if(response.equals("Товар не найден.")){
                    response = "Товар не найден или штрихкода нет в базе.";
                }

                SendMessage outMess = new SendMessage();

                outMess.setChatId(chatId);
                outMess.setText(response);
                outMess.setReplyMarkup(replyKeyboardMarkup);

                execute(outMess);

            }

            if(update.hasMessage() && update.getMessage().hasText())
            {

                //Извлекаем из объекта сообщение пользователя
                Message inMess = update.getMessage();
                //Достаем из inMess id чата пользователя
                String chatId = inMess.getChatId().toString();
                //Получаем текст сообщения пользователя, отправляем в написанный нами обработчик
                String response = parseMessage(inMess.getText());
                //Создаем объект класса SendMessage - наш будущий ответ пользователю
                SendMessage outMess = new SendMessage();

                //Добавляем в наше сообщение id чата а также наш ответ
                outMess.setChatId(chatId);
                outMess.setText(response);
                outMess.setReplyMarkup(replyKeyboardMarkup);

                //Отправка в чат
                execute(outMess);
            }

            if (update.getMessage().hasVoice()) {
                Message inMess = update.getMessage();
                Voice audio = inMess.getVoice();
                String fileId = audio.getFileId();
                String filePath = downloadVoiceFile(fileId);
                String text = convertVoiceToText(filePath);

                String chatId = inMess.getChatId().toString();
                SendMessage outMess = new SendMessage();

                if (text.equals("")) text = "Речь не распознана.";


                //Добавляем в наше сообщение id чата а также наш ответ
                outMess.setChatId(chatId);
                outMess.setText(text);
                outMess.setReplyMarkup(replyKeyboardMarkup);

                //Отправка в чат
                execute(outMess);
            }
        } catch (TelegramApiException | IOException e) {
            //Обработка ошибки связанной с достижением лимита одного сообщения
            if(e.getLocalizedMessage().equals("Error sending message: [400] Bad Request: message is too long")){

                Message inMess = update.getMessage();
                String chatId = inMess.getChatId().toString();
                String response = "Слишком много совпадений. Пожалуйста укажите более подробную фразу поиска.";

                SendMessage outMess = new SendMessage();

                outMess.setChatId(chatId);
                outMess.setText(response);
                outMess.setReplyMarkup(replyKeyboardMarkup);

                //Отправка в чат
                try {
                    execute(outMess);
                } catch (TelegramApiException telegramApiException) {
                    telegramApiException.printStackTrace();
                }

            }
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    public String parseMessage(String textMsg) {
        String response;

        //Сравниваем текст пользователя с нашими командами, на основе этого формируем ответ

        switch (textMsg){
            case "/start" : response = "Чат-бот магазина 'Инструмент и крепежПриветствую'. Бот ищет цену товара по названию.";
                System.out.println(response);
                break;
            case "/shop sev" : response = "Установлена зона поиска: магазин Севастополь.";
                System.out.println(response);
                shop = "Розница_Севастополь";
                break;
            case "/shop sim" : response = "Установлена зона поиска: магазин Симферополь.";
                System.out.println(response);
                shop = "Розница";
                break;
            case "/shop all" : response = "Установлена зона поиска: Все магазины.";
                shop = "all";
                break;
            default : response = findProduct(textMsg);
        }

        /*if (textMsg.equals("/start")) {
            response = "Чат-бот магазина 'Инструмент и крепежПриветствую'. Бот ищет цену товара по названию.";
        }
        else{
            //response = "Сообщение не распознано";
            response = findProduct(textMsg);
        }*/

        return response;
    }

    //клавиатура
    void initKeyboard()
    {
        //Создаем объект будущей клавиатуры и выставляем нужные настройки
        replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подгоняем размер
        replyKeyboardMarkup.setOneTimeKeyboard(false); //скрываем после использования

        //Создаем список с рядами кнопок
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<KeyboardRow>();
        //Создаем один ряд кнопок и добавляем его в список
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRows.add(keyboardRow);
        //Добавляем одну кнопку с текстом "Просвяти" наш ряд
        keyboardRow.add(new KeyboardButton("Просвяти"));
        //добавляем лист с одним рядом кнопок в главный объект
        replyKeyboardMarkup.setKeyboard(keyboardRows);
    }

    String findProduct(String search_string){

        int i = 6;  //начало номенклатуры
        String result = "";
        while(!excel.getCell(i, 1).toString().equals("")){

            //проверка принадлежности товара к разным магазинам
            if(!shop.equals("all")) {
                if (!excel.getCell(i, 0).toString().equals(shop)) {
                    i++;
                    continue;
                }
            }

            String [] search_expression = search_string.split(" ");
            int counter = 0;

            //проверяем каждое слово из искомого выражения. В искомом тексте должны быть эти слова в любом порядке
            for(int j = 0; j < search_expression.length; j++){
                if(excel.getCell(i, 0).toString().toLowerCase().contains(search_expression[j].toLowerCase())){
                    counter++;
                }
            }
            if(counter == search_expression.length){    //проверка, все ли слова есть в искомом выражении
                result += "\uD83C\uDF0D" + excel.getCell(i, 0).toString() + "\n" + " Цена: " + excel.getCell(i, 4).toString() + " Руб." + "\n\n";    //возврат наименование + цена
            }

            i++;
        }
        if(result.equals("")){
            return "Товар не найден.";
        }else{
            System.out.println(result);
            return result;
        }

    }

    public String findProductFromBarCode(String BarCode){

        int i = 7;  //начало номенклатуры
        String result = "";
        while(!BARCODE.getCell(i, 0).toString().equals("")){

            if(BARCODE.getCell(i, 0).toString().equals(BarCode)){
                return BARCODE.getCell(i, 3).toString();
            }

            i++;

        }

        return "Штрихкод не найден!";
    }

    public InputStream downloadImg(String name) throws IOException {

        URL url = new URL(name);
        URLConnection c = url.openConnection();
        return (c.getInputStream());

    }

    public String getFilePath(String FileID){

        String text = null;
        try (Scanner scanner = new Scanner(downloadImg("https://api.telegram.org/bot" + getBotToken() + "/getfile?file_id=" + FileID), StandardCharsets.UTF_8.name())) {
            text = scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(text);
        //System.out.println(text.substring(text.indexOf("file_path") + "file_path".length() + 2).replaceAll("}", "").replaceAll("\"", ""));

        return text.substring(text.indexOf("file_path") + "file_path".length() + 2).replaceAll("}", "").replaceAll("\"", "");
    }

    private String downloadVoiceFile(String fileId) {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);
        try {
            org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(getFileMethod);
            String fileUrl = getFileUrl(telegramFile);
            try (InputStream in = new URL(fileUrl).openStream()) {
                Path filePath = Files.createTempFile("voice_", ".oga");
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                return filePath.toString();
            }
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFileUrl(org.telegram.telegrambots.meta.api.objects.File file) {
        return String.format("https://api.telegram.org/file/bot%s/%s", getBotToken(), file.getFilePath());
    }

    private String convertVoiceToText(String filePath) throws IOException, InstantiationException {
        Configuration configuration = new Configuration();

        // Установка пути к модели распознавания речи
        configuration.setAcousticModelPath("file:cmusphinx-ru-5.2");
        configuration.setDictionaryPath("file:cmusphinx-ru-5.2\\ru.dic");
        configuration.setLanguageModelPath("file:cmusphinx-ru-5.2\\ru.lm");

        InputStream audioStream = null;

        audioStream = new FileInputStream(filePath);
        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        System.out.println("Метка ошибки");

        recognizer.startRecognition(audioStream);
        String result = recognizer.getResult().getHypothesis();

        recognizer.stopRecognition();
        audioStream.close();

        return result;
    }


}