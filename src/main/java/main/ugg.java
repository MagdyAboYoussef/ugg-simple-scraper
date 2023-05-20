package main;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ugg {
    Map<String, Map<String, String>> nameServer;
    EdgeDriver driver;
    JavascriptExecutor js;
    Map<String, Map<String, Integer>> championWinLoss;
    Workbook workbook;
    Sheet sheet;
    int currentRow;
    boolean headless, update, ranked;

    ugg(Map<String, Map<String, String>> nameServer, String excel, boolean headless, boolean selected, boolean ranked) {
        this.nameServer = nameServer;
        this.headless = headless;
        this.update = selected;
        this.ranked = ranked;

        setUpWebDriver();
        createExcelSheet();

        for (Map.Entry<String, Map<String, String>> entry : nameServer.entrySet()) {
            String key = entry.getKey();
            Map<String, String> value = entry.getValue();
            String name = key;
            String server = value.get("server");
            String role = value.get("role");
            Map<String, Map<String, Integer>> excelStats = getStats(name, server, role, update, ranked);
        }

        writeExcelFile(excel);
        closeResources();
    }

    private void setUpWebDriver() {
        if (headless) {
            WebDriverManager.edgedriver().setup();
            EdgeOptions chromeOptions = new EdgeOptions();
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("disable-gpu");
            driver = new EdgeDriver(chromeOptions);
        } else {
            WebDriverManager.edgedriver().setup();
            driver = new EdgeDriver();
            driver.manage().window().maximize();
        }

        js = (JavascriptExecutor) driver;
    }

    private void createExcelSheet() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Champion Stats");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Summoner name");
        headerRow.createCell(1).setCellValue("Champion Name");
        headerRow.createCell(2).setCellValue("Wins");
        headerRow.createCell(3).setCellValue("Losses");
        headerRow.createCell(4).setCellValue("Win ratio");

        currentRow = 1;
    }

    private void writeExcelFile(String excel) {
        try (FileOutputStream fileOut = new FileOutputStream(excel + ".xlsx")) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeResources() {
        // Close the workbook and driver
        try {
            driver.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map<String, Integer>> getStats(String name, String server, String role, boolean update, boolean ranked) {
        goToProfilePage(name, server, update);

        selectRankedModeIfNecessary(ranked);
        List<WebElement> matches = getAllMatches();

        championWinLoss = processMatches(matches);

        int total = updateExcelSheet(name, role);

        addExtraRows(total);

        // Resize all columns to fit the content size
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        return championWinLoss;
    }

    private void goToProfilePage(String name, String server, boolean update) {
        driver.get("https://u.gg/lol/profile/" + server + "/" + name + "/overview");
        if (update) {
            driver.findElement(By.className("update-button")).click();
            sleep(5000);
            driver.get("https://u.gg/lol/profile/" + server + "/" + name + "/overview");
        }
    }

    private void selectRankedModeIfNecessary(boolean ranked) {
        if (ranked) {
            sleep(1500);
            WebElement element = driver.findElement(By.cssSelector(".default-select__value-container.default-select__value-container--has-value.css-1kuy7z7"));
            element.click();
            sleep(1500);
            WebElement option = driver.findElement(By.xpath("//div[@class='default-select__option css-1kuy7z7' and contains(text(), 'Ranked Solo')]"));
            option.click();
            sleep(1000);
        }
    }

    private List<WebElement> getAllMatches() {
        List<WebElement> matches = driver.findElements(By.className("match-history_match-card"));
        int y = 0;
        while (matches.size() < 50) {
            int x = matches.size();
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            matches = driver.findElements(By.className("match-history_match-card"));
            if (x == matches.size()) {
                y++;
            } else {
                y = 0;
            }
            if (y >= 300) {
                break;
            }
        }
        return matches;
    }

    private Map<String, Map<String, Integer>> processMatches(List<WebElement> matches) {
        Map<String, Map<String, Integer>> championWinLoss = new HashMap<>();
        int x = 0;
        for (int i = 0; i < matches.size(); i++) {
            if (x > 50) {
                break;
            }
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", matches.get(i));
            WebElement element = matches.get(i).findElement(By.className("large-match-card-container"));
            String classAttribute = element.getAttribute("class");
            element = element.findElement(By.className("match-summary_desktop"));

            WebElement imgElement = element.findElement(By.cssSelector("div.champion-face img"));
            String championName = imgElement.getAttribute("alt");
            System.out.println(i + " " + classAttribute + " " + championName);

            championWinLoss = updateChampionStats(championWinLoss, classAttribute, championName);
            x += 1;
        }
        return championWinLoss;
    }

    private Map<String, Map<String, Integer>> updateChampionStats(Map<String, Map<String, Integer>> championWinLoss, String classAttribute, String championName) {
        // Check if the champion is already in the map
        if (championWinLoss.containsKey(championName)) {
            Map<String, Integer> winLossMap = championWinLoss.get(championName);

            // Check the class attribute to determine win or loss
            if (classAttribute.contains("match_win")) {
                int wins = winLossMap.getOrDefault("Wins", 0);
                winLossMap.put("Wins", wins + 1);
            } else if (classAttribute.contains("match_lose")) {
                int losses = winLossMap.getOrDefault("Losses", 0);
                winLossMap.put("Losses", losses + 1);
            }
        } else {
            // Create a new entry in the map for the champion
            Map<String, Integer> winLossMap = new HashMap<>();
            if (classAttribute.contains("match_win")) {
                winLossMap.put("Wins", 1);
                winLossMap.put("Losses", 0);
            } else if (classAttribute.contains("match_lose")) {
                winLossMap.put("Wins", 0);
                winLossMap.put("Losses", 1);
            }

            championWinLoss.put(championName, winLossMap);
        }
        return championWinLoss;
    }

    private int updateExcelSheet(String name, String role) {
        int total = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : championWinLoss.entrySet()) {
            if (entry.getValue().get("Wins") != null && entry.getValue().get("Losses") != null) {
                double wins = entry.getValue().get("Wins");
                double losses = entry.getValue().get("Losses");
                total += (int) (wins + losses);
                Row row = sheet.createRow(currentRow++);
                row.createCell(0).setCellValue(name + "[" + role + "]");
                row.createCell(1).setCellValue(entry.getKey());
                row.createCell(2).setCellValue(wins);
                row.createCell(3).setCellValue(losses);
                DecimalFormat decimalFormat = new DecimalFormat("0.00");

                row.createCell(4).setCellValue(decimalFormat.format(((wins / (losses + wins)) * 100)) + "%");
                row.createCell(5).setCellValue((int) (wins + losses) + " Games");
            }
        }
        return total;
    }

    private void addExtraRows(int total) {
        Row row = sheet.createRow(currentRow++);
        row.createCell(5).setCellValue("Total games: " + total);
        row = sheet.createRow(currentRow++);
        row = sheet.createRow(currentRow++);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
