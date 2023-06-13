package sample;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimilarPositionsFinder {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\Kassa_1\\Desktop\\test\\TestText.xlsx"; // Путь к файлу Excel
        String sheetName = "Лист1"; // Название листа с данными
        int columnNumber = 1; // Номер столбца для сравнения
        String searchString = "рулетка 5"; // Строка для сравнения
        int distanceThreshold = 20; // Пороговое значение расстояния Левенштейна

        try {
            List<String> similarStrings = findSimilarStringsInExcel(filePath, sheetName, columnNumber, searchString, distanceThreshold);
            System.out.println("Близкие строки:");
            for (String similarString : similarStrings) {
                System.out.println(similarString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> findSimilarStringsInExcel(String filePath, String sheetName, int columnNumber, String searchString, int distanceThreshold) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet(sheetName);

        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        List<String> similarStrings = new ArrayList<>();

        for (Row row : sheet) {
            Cell cell = row.getCell(columnNumber - 1);

            if (cell != null && cell.getCellType() == CellType.STRING) {
                String cellValue = cell.getStringCellValue();
                int distance = levenshteinDistance.apply(searchString, cellValue);

                if (distance <= distanceThreshold) {
                    similarStrings.add(cellValue);
                }
            }
        }

        workbook.close();
        fis.close();

        return similarStrings;
    }
}

