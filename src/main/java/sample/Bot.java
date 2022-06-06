package sample;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;

public class Bot extends TelegramLongPollingBot {
    //создаем две константы, присваиваем им значения токена и имя бота соответсвтенно
    //вместо звездочек подставляйте свои данные
    final private String BOT_TOKEN = "5401035487:AAEQlxp44vLeiMu9lHUI6dDw51xqBg8u9gE";
    final private String BOT_NAME = "TestIDEytevv_bot";
    ReplyKeyboardMarkup replyKeyboardMarkup;
    private String shop = "all";

    Excel excel = new Excel();

    Bot()
    {
        excel.createExcel("C:\\Users\\Admin\\Desktop\\Номенклатура_05.06.22.xlsx", 0);
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
        } catch (TelegramApiException e) {
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

        int i = 9;  //начало номенклатуры
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
                if(excel.getCell(i, 1).toString().toLowerCase().contains(search_expression[j].toLowerCase())){
                    counter++;
                }
            }
            if(counter == search_expression.length){    //проверка, все ли слова есть в искомом выражении
                result += "\uD83C\uDF0D" + excel.getCell(i, 1).toString() + "\n" + " Цена: " + excel.getCell(i, 5).toString() + " Руб." + "\n\n";    //возврат наименование + цена
            }

            i++;
        }
        if(result.equals("")){
            return "Товар не найден";
        }else{
            System.out.println(result);
            return result;
        }

    }

}