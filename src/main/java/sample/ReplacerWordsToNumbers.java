package sample;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacerWordsToNumbers {
    private static final Map<String, Integer> numberWords = new HashMap<>();

    static {
        numberWords.put("ноль", 0);
        numberWords.put("один", 1);
        numberWords.put("два", 2);
        numberWords.put("три", 3);
        numberWords.put("четыре", 4);
        numberWords.put("пять", 5);
        numberWords.put("шесть", 6);
        numberWords.put("семь", 7);
        numberWords.put("восемь", 8);
        numberWords.put("девять", 9);
        numberWords.put("десять", 10);
        numberWords.put("одиннадцать", 11);
        numberWords.put("двенадцать", 12);
        numberWords.put("тринадцать", 13);
        numberWords.put("четырнадцать", 14);
        numberWords.put("пятнадцать", 15);
        numberWords.put("шестнадцать", 16);
        numberWords.put("семнадцать", 17);
        numberWords.put("восемнадцать", 18);
        numberWords.put("девятнадцать", 19);
        numberWords.put("двадцать", 20);
        numberWords.put("тридцать", 30);
        numberWords.put("сорок", 40);
        numberWords.put("пятьдесят", 50);
        numberWords.put("шестьдесят", 60);
        numberWords.put("семьдесят", 70);
        numberWords.put("восемьдесят", 80);
        numberWords.put("девяносто", 90);
        numberWords.put("сто", 100);
        numberWords.put("двести", 200);
        numberWords.put("триста", 300);
        numberWords.put("четыреста", 400);
        numberWords.put("пятьсот", 500);
        numberWords.put("шестьсот", 600);
        numberWords.put("семьсот", 700);
        numberWords.put("восемьсот", 800);
        numberWords.put("девятьсот", 900);
        numberWords.put("тысяча", 1000);
        numberWords.put("тысячи", 1000);
        numberWords.put("тысяч", 1000);
        numberWords.put("миллион", 1000000);
        numberWords.put("миллиона", 1000000);
        numberWords.put("миллионов", 1000000);
    }

    public static void main(String[] args) {
        String text = "Вчера было двадцать пять градусов по Цельсию. Один миллион сто тридцать три прогулялись в парке.";
        String result = replaceNumberWordsWithDigits(text);
        System.out.println(result);
    }

    public static String replaceNumberWordsWithDigits(String text) {
        Pattern pattern = Pattern.compile("\\b([а-яА-Я]+)\\b");
        Matcher matcher = pattern.matcher(text);

        StringBuilder builder = new StringBuilder();
        int previousEnd = 0;

        while (matcher.find()) {
            String word = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            builder.append(text, previousEnd, start);

            if (isNumberWord(word)) {
                int number = parseNumber(word);
                builder.append(number);
            } else {
                builder.append(word);
            }

            previousEnd = end;
        }

        builder.append(text.substring(previousEnd));

        return builder.toString();
    }

    public static boolean isNumberWord(String word) {
        return numberWords.containsKey(word.toLowerCase());
    }

    public static int parseNumber(String numberInWords) {
        String[] words = numberInWords.split(" ");
        int number = 0;
        int currentNumber = 0;
        int previousNumber = 0;

        for (String word : words) {
            if (isNumberWord(word)) {
                currentNumber = numberWords.get(word.toLowerCase());
                if (currentNumber == 0 && (previousNumber == 0 || previousNumber >= 100)) {
                    continue;
                }
                number += currentNumber;
            }
            previousNumber = currentNumber;
        }

        return number;
    }
}

