package com.sirion.util;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.json.JsonPath;

public class TestUtil {

	static ArrayList<String> al = new ArrayList<>();
	static Cell idCell;
	static Workbook workbook;
	static Cell status;
	static ArrayList<String> fieldValues = new ArrayList<>();
	static String fieldVal;
	static Sheet newSheet;
	static String superAdminAccessToken = null;
	static String superAdminURL = null;
	static String click = "arguments[0].click();";

	public static String getEncryptedPassword(String password) {

		return DigestUtils.md5Hex(password).toLowerCase();
	}

	public static Properties ReadPropertiesFile(Properties properties, String proerties_File_Path) {
		try {
			FileInputStream fileInputStream = new FileInputStream(proerties_File_Path);
			properties.load(fileInputStream);
			fileInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}

	public static String getToken(Properties properties) {

		String loginEndpoint = TestUtil.getInfo(properties, "loginAPI");
		String BaseURL = TestUtil.getInfo(properties, "BaseURL");
		String username = TestUtil.getInfo(properties, "Username");
		String password = TestUtil.getInfo(properties, "Password");
		String encryptedPswd = TestUtil.getEncryptedPassword(password);
		RestAssured.baseURI = BaseURL;
		RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
		String loginResponse = given().contentType("application/x-www-form-urlencoded; charset=utf-8")
				.formParam("j_username", username).formParam("j_password", encryptedPswd).when().post(loginEndpoint)
				.then().extract().response().asString();
		JsonPath jsonPath = new JsonPath(loginResponse);
		String token = jsonPath.getString("accessToken");
		System.out.println("Token for URL " + BaseURL + " is : ");
		System.out.println(token);
		System.out.println();
		return token;
	}

	public static void getLogin(WebDriver driver, Properties properties) {
		driver.manage().window().maximize();
		String BaseURL = TestUtil.getInfo(properties, "BaseURL");
		String username = TestUtil.getInfo(properties, "Username");
		String password = TestUtil.getInfo(properties, "Password");
		driver.get(BaseURL);
		driver.findElement(By.name("username_login")).sendKeys(username);
		driver.findElement(By.name("password_login")).sendKeys(password);
		driver.findElement(By.id("loginButton")).click();
	}

	public static WebDriver initializeWebDriver() {
		WebDriverManager.chromedriver().setup();
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
		return driver;
	}

	public static String getInfo(Properties properties, String string) {
		return properties.getProperty(string);
	}

	public static String getCellValues(String refIDfilePath, int rowIndex, int cellIndex) {

		try {
			FileInputStream fis = new FileInputStream(new File(refIDfilePath));
			Workbook workbook = WorkbookFactory.create(fis);
			Sheet sheet = workbook.getSheetAt(0);
			Row row = sheet.getRow(rowIndex);
			if (row != null) {
				Cell cell = row.getCell(cellIndex);
				if (cell != null && cell.getCellType() == CellType.STRING) {
					fieldVal = cell.getStringCellValue();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fieldVal;
	}

	public static int generateRandomNumber() {
		Random random = new Random();
		return random.nextInt(900) + 100;
	}

	public static void cloneExcelFile(String input, String output) {
		File sourceExcel = new File(input);
		File dstExcel = new File(output);
		try {
			FileUtils.copyFile(sourceExcel, dstExcel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> getFieldValues(String refIDfilePath) {
		try {
			FileInputStream fis = new FileInputStream(new File(refIDfilePath));
			Workbook workbook = WorkbookFactory.create(fis);
			Sheet sheet = workbook.getSheetAt(0);
			int rowCount = sheet.getPhysicalNumberOfRows();
			for (int i = 1; i < rowCount; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cell = row.getCell(0);
					if (cell != null && cell.getCellType() == CellType.STRING) {
						String fieldVal = cell.getStringCellValue();
						fieldValues.add(fieldVal);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fieldValues;
	}

	public static void addStatusColumn(String columnName, String excelFilePath, int cellIndex) {
		try {
			FileInputStream fis = new FileInputStream(excelFilePath);
			Workbook workbook = new XSSFWorkbook(fis);
			Sheet mysheet = workbook.getSheetAt(0);
			Row myRow = mysheet.getRow(0);
			Cell myCreatedCell = myRow.createCell(cellIndex);
			myCreatedCell.setCellValue(columnName);
			FileOutputStream outputStream = new FileOutputStream(excelFilePath);
			workbook.write(outputStream);
			workbook.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveValuesInExcel(String excelFilePath) throws IOException {
		workbook = new XSSFWorkbook();
		newSheet = workbook.createSheet();
		int rowNum = 0;
		// Create header row with column names
		Row headerRow = newSheet.createRow(rowNum++);
		// Define column names
		String[] columnNames = { "EBR ID", "Rule Data", "ActionData" };
		for (int i = 0; i < columnNames.length; i++) {
			Cell headerCell = headerRow.createCell(i);
			headerCell.setCellValue(columnNames[i]);
		}
		// Populate data rows
		for (String fieldValue : fieldValues) {
			if (!(fieldValue.isEmpty())) {
				Row row = newSheet.createRow(rowNum++);
				Cell fieldVal = row.createCell(0);
				fieldVal.setCellValue(fieldValue);
			}
		}
		FileOutputStream outputStream = new FileOutputStream(excelFilePath);
		workbook.write(outputStream);
		workbook.close();
		outputStream.close();
	}

	public static void saveEBRIDs(String ebrID, String excelFilePath, int rowIndex, int cellIndex) {
		try {
			FileInputStream fis = new FileInputStream(excelFilePath);
			workbook = new XSSFWorkbook(fis);
			newSheet = workbook.getSheetAt(0);
			Row row = newSheet.createRow(rowIndex);
			Cell myCell = row.createCell(cellIndex);
			myCell.setCellValue(ebrID);
			FileOutputStream outputStream = new FileOutputStream(excelFilePath);
			workbook.write(outputStream);
			workbook.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveRuleData(List<String> actionDataResponse, String excelFilePath, int rowIndex,
			int cellIndex) {
		try {
			FileInputStream fis = new FileInputStream(excelFilePath);
			workbook = new XSSFWorkbook(fis);
			newSheet = workbook.getSheetAt(0);
			Row row = newSheet.getRow(rowIndex);
			Cell myCell = row.createCell(cellIndex);
			for (int i = 0; i < actionDataResponse.size(); i++) {
				if (myCell.getStringCellValue() != null && !myCell.getStringCellValue().isEmpty()) {
					String exists = myCell.getStringCellValue();
					myCell.setCellValue(exists + ", " + actionDataResponse.get(i));
				} else {
					myCell.setCellValue(actionDataResponse.get(i));
				}
			}
			FileOutputStream outputStream = new FileOutputStream(excelFilePath);
			workbook.write(outputStream);
			workbook.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int updateActiveStatus(WebDriver driver, JavascriptExecutor executor, String updateActiveStatus,
			int rowIndexForRule, String outputFilePath, WebDriverWait wait) throws InterruptedException {

		By byXpath = By.xpath("(//mat-label[contains(text(), 'Active')]/following::label//span)[1]");
		wait.until(ExpectedConditions.visibilityOfElementLocated(byXpath));
		String value = driver.findElement(byXpath).getAttribute("title");
		if (!value.equalsIgnoreCase(updateActiveStatus)) {
			Thread.sleep(5000);
			executor.executeScript(click, driver.findElement(By.xpath("//button[@title=\"Edit\"]")));
			executor.executeScript(click, driver.findElement(By.xpath(
					"//span[@class='mat-checkbox-inner-container mat-checkbox-inner-container-no-side-margin']")));
			executor.executeScript(click, driver.findElement(By.xpath("//button[@title=\"Update\"]")));
			wait.until(ExpectedConditions.visibilityOfElementLocated(byXpath));
			value = driver.findElement(byXpath).getAttribute("title");
			if (value.equalsIgnoreCase(updateActiveStatus)) {
				String status = "Success";
				saveStatusInExcel(status, rowIndexForRule, outputFilePath, 1);
			} else {
				String status = "Unable to update";
				saveStatusInExcel(status, rowIndexForRule, outputFilePath, 1);
			}
		} else {
			String status = "Already " + updateActiveStatus;
			saveStatusInExcel(status, rowIndexForRule, outputFilePath, 1);
		}
		return rowIndexForRule;
	}

	public static void saveStatusInExcel(String status, int rowIndexForRule, String outputFilePath, int cellIndex) {
		try {
			FileInputStream fis = new FileInputStream(outputFilePath);
			try (Workbook workbook = new XSSFWorkbook(fis)) {
				Sheet mysheet = workbook.getSheetAt(0);
				Row myRow = mysheet.getRow(rowIndexForRule);
				if (myRow != null) {
					Cell myCreatedCell = myRow.createCell(cellIndex);
					myCreatedCell.setCellValue(status);
				} else {
					myRow = mysheet.createRow(rowIndexForRule);
					Cell myCreatedCell = myRow.createCell(cellIndex);
					myCreatedCell.setCellValue(status);
				}
				FileOutputStream outputStream = new FileOutputStream(outputFilePath);
				workbook.write(outputStream);
				workbook.close();
				outputStream.close();
			}
			System.out.println("Status : " + status + " saved");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
