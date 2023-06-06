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
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
//import org.telegram.telegrambots.meta.api.objects.File;
import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;

import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.LibVosk;
import org.vosk.Model;

import javax.sound.sampled.AudioSystem;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;


public class Bot extends TelegramLongPollingBot {
    //создаем две константы, присваиваем им значения токена и имя бота соответсвтенно
    //вместо звездочек подставляйте свои данные
    final private String BOT_TOKEN = "5401035487:AAEQlxp44vLeiMu9lHUI6dDw51xqBg8u9gE";
    final private String BOT_NAME = "TestIDEytevv_bot";
    final private String PathVoskModel = "C:\\Users\\Cashless\\IdeaProjects\\TelegramBot\\vosk-model-small-ru-0.22";
    final private String PathFfmpeg = "C:\\Users\\Cashless\\IdeaProjects\\TelegramBot\\ffmpeg-5.1.2-full_build\\bin\\ffmpeg.exe";
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
                String response = "";


                response = VOSK(convertOgaToWav(filePath));
                System.out.println("Распознанный текст: " + response);

                String chatId = inMess.getChatId().toString();
                SendMessage outMess = new SendMessage();

                if (response.equals(" ")) {
                    response = "Речь не распознана.";
                } else {
                    response = findProduct(response);
                }


                //Добавляем в наше сообщение id чата а также наш ответ
                outMess.setChatId(chatId);
                outMess.setText(response);
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
        } catch (UnsupportedAudioFileException e) {
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
                //Path filePath = Files.createTempFile("voice_", ".oga");
                Path filePath = Files.createTempFile("voice_", ".ogg");
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

    //распознование речи с помощью VOSK
    private String VOSK(String filePath) throws IOException, UnsupportedAudioFileException {

        LibVosk.setLogLevel(LogLevel.DEBUG);
        String result = "";

        try (Model model = new Model(PathVoskModel);
             InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(filePath)));
             //Для корректной работы нужно подбирать sampleRate
             Recognizer recognizer = new Recognizer(model, 40000)) {

            int nbytes;
            byte[] b = new byte[16384]; //увеличивая размер буфера можно увеличить скорость
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    result = result + CR(recognizer.getResult()) + " ";
                    System.out.println(result);
                } else {
                    System.out.println(recognizer.getPartialResult());
                }
            }
            result = result.substring(0, result.length() - 1);

            System.out.println("FinalResult: " + recognizer.getFinalResult());
            System.out.println("----->>>" + result);
        }

        return result;
    }

    //обрезает лишнее в итоговой строке после работы VOSK
    private String CR(String str) {

        String start  = "\"text\" : \"";
        String finish = "\"\n}";
        return str.substring(str.indexOf(start) + start.length(), str.indexOf(finish));

    }

    //принимает файл формата oga и конвертирует его в wav
    private String convertOgaToWav(String ogaFilePath) {

        try {
            // Создаем временной WAV файл
            File tempWavFile = File.createTempFile("output", ".wav");
            String tempWavName = tempWavFile.getAbsolutePath();
            tempWavFile.delete();

            // Команда FFmpeg для конвертации OGA в WAV
            String[] ffmpegCommand = {
                    PathFfmpeg,
                    "-i",
                    ogaFilePath,
                    "-af",
                    "\"apad=whole_dur=10\"",    //добавляем 10 секунд иначе vosk виснет
                    tempWavName
            };

            // Выполняем команду FFmpeg
            //Process ffmpegProcess = Runtime.getRuntime().exec(ffmpegCommand);

            // Создаем процесс FFmpeg
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
            Process ffmpegProcess = processBuilder.start();

            // Читаем вывод процесса в отдельном потоке
            StreamGobbler outputGobbler = new StreamGobbler(ffmpegProcess.getInputStream());
            outputGobbler.start();

            // Читаем ошибки процесса в отдельном потоке
            StreamGobbler errorGobbler = new StreamGobbler(ffmpegProcess.getErrorStream());
            errorGobbler.start();

            System.out.println("PATH: " + tempWavName);
            System.out.println("ffmpegCommand: " + ffmpegCommand[0] + " " +  ffmpegCommand[1] + " " +  ffmpegCommand[2] + " " +  ffmpegCommand[3]);
            int exitCode = ffmpegProcess.waitFor();

            if (exitCode == 0) {
                // Конвертация прошла успешно
                return tempWavName;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}

// Вспомогательный класс для чтения вывода и ошибок процесса в отдельных потоках
class StreamGobbler extends Thread {
    private final InputStream inputStream;

    public StreamGobbler(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Читаем и обрабатываем вывод или ошибку процесса
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}