package com.sirion.testcases;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sirion.util.TestUtil;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class EBR_Automation {

	static final String Proerties_File_Path = "./src/main/java/com/sirion/config/config.properties";
	static Properties property = new Properties();
	static Properties properties = TestUtil.ReadPropertiesFile(property, Proerties_File_Path);
	static final String payload = "{\r\n" + "    \"filterMap\": {}\r\n" + "}";
	static final String Token = TestUtil.getToken(properties);
	static final String INPUT_FILE_PATH = TestUtil.getInfo(properties, "InputFilePath");
	static final String OUTPUT_FILE_PATH = TestUtil.getInfo(properties, "OutputFilePath");
	static final String showEventsEndpointValue = TestUtil.getInfo(properties, "showEvents");
	static final String deleteEventEndpoint = TestUtil.getInfo(properties, "deleteEvent");
	static final String fetchEBR = TestUtil.getInfo(properties, "fetchEBRData");
	static final String deleteEBR = TestUtil.getInfo(properties, "deleteEBR");
	static final String updateEBR = TestUtil.getInfo(properties, "updateEBRData");

	public static void main(String[] args) throws IOException, InterruptedException {

		String path = "uxadmin/#/workflow/eventEngine/";
		String url = TestUtil.getInfo(properties, "BaseURL");
		JsonPath jsonpath;
		TestUtil.saveValuesInExcel(OUTPUT_FILE_PATH);
		Response Response = given().contentType("application/json").header("Authorization", Token).body(payload).when()
				.post(TestUtil.getInfo(properties, "listData")).then().extract().response();
		jsonpath = new JsonPath(Response.asString());
		ArrayList<String> ebrIDs = new ArrayList<String>();
		ArrayList<String> refIDs = new ArrayList<String>();
		int size = jsonpath.getInt("data.size()");
		int rowIndexForEBRID = 1;
		int rowIndexForRule = 1;
		for (int i = 0; i < size; i++) {
			String id = jsonpath.get("data[" + i + "].19221.value");
			String[] data = id.split(":;");
			ebrIDs.add(data[0]);
			refIDs.add(data[1]);
		}
		System.out.println("EBR IDs are : " + ebrIDs);
		System.out.println("Ref IDs are : " + refIDs);
		saveEBRIds(ebrIDs, rowIndexForEBRID);
		if (fetchEBR.equalsIgnoreCase("Yes")) {
			fetchData(refIDs, jsonpath, rowIndexForRule);
		}
		if (deleteEBR.equalsIgnoreCase("Yes")) {
			deleteEBR(ebrIDs, refIDs, url, path, rowIndexForRule);
		}
		if (updateEBR.equalsIgnoreCase("Yes")) {
			updateEBR(ebrIDs, refIDs, url, path, rowIndexForRule);
		}
	}

	public static void saveEBRIds(ArrayList<String> ebrIDs, int rowIndexForEBRID) {
		for (int ebrID = 0; ebrID < ebrIDs.size(); ebrID++) {
			TestUtil.saveEBRIDs(ebrIDs.get(ebrID), OUTPUT_FILE_PATH, rowIndexForEBRID, 0);
			rowIndexForEBRID++;
		}
	}

	public static Response showEvents(ArrayList<String> refIDs, int refID) {
		String endpoint = showEventsEndpointValue + refIDs.get(refID) + "";
		return given().contentType("application/json").header("Cookie", "Authorization=" + Token + "").when()
				.get(endpoint).then().assertThat().statusCode(200).extract().response();
	}

	public static Response deleteEBRID(String refId) {
		String endpoint = showEventsEndpointValue + refId;
		return given().contentType("application/json").header("Cookie", "Authorization=" + Token + "").when()
				.get(endpoint).then().assertThat().statusCode(200).extract().response();
	}

	public static void ruleData(JsonPath jsonpath, int rowIndexForRule, String outputFilePath) {
		String ruleData = jsonpath.get("data.simplifiedRule");
		ArrayList<String> rule = new ArrayList<>();
		rule.add(ruleData);
		TestUtil.saveRuleData(rule, outputFilePath, rowIndexForRule, 1);
		System.out.println("Rule saved : " + ruleData);
	}

	public static void actionData(JsonPath jsonpath, int rowIndexForRule, String outputFilePath) {
		if (jsonpath.get("data.actionData") != null) {
			String actionData = jsonpath.getJsonObject("data.actionData").toString();
			jsonpath = new JsonPath(actionData);
			ArrayList<String> actionDataResponse = new ArrayList<>();
			if (jsonpath.get("valueUpdate") != null) {
				String updateOperation = null;
				String updateField = null;
				int valuesUpdate = jsonpath.getInt("valueUpdate.size()");
				for (int i = 0; i < valuesUpdate; i++) {
					updateOperation = jsonpath.getString("valueUpdate[" + i + "].mode.name");
					updateField = jsonpath.getString("valueUpdate[" + i + "].field.name");
					Object value = jsonpath.get("valueUpdate[" + i + "].value");
					ArrayList<String> valueTo = new ArrayList<>();
					// Check if the value is an object
					if (value instanceof Map) {
						valueTo.add(jsonpath.get("valueUpdate[" + i + "].value.name"));
					} else {
						int valueSize = jsonpath.get("valueUpdate[" + i + "].value.size()");
						for (int z = 0; z < valueSize; z++) {
							valueTo.add(jsonpath.getString("valueUpdate[" + i + "].value[" + z + "].name"));
						}
					}
					actionDataResponse.add("{updateOperation : " + updateOperation + ", updateField : " + updateField
							+ ", valueTo : " + valueTo + "}");
				}
			}
			if (jsonpath.get("task") != null) {
				String UpdateStatusTo = jsonpath.getString("task.name");
				actionDataResponse.add("{UpdateStatusTo : " + UpdateStatusTo + "}");
			}
			if (jsonpath.get("apiId") != null) {
				String APIURL = jsonpath.getString("apiId.name");
				actionDataResponse.add("{API URL : " + APIURL + "}");
			}
			TestUtil.saveRuleData(actionDataResponse, outputFilePath, rowIndexForRule, 2);
			System.out.println("Action Data saved : " + actionDataResponse);
		}
	}

	public static void fetchData(ArrayList<String> refIDs, JsonPath jsonpath, int rowIndexForRule) {
		for (int refIDIndex = 0; refIDIndex < refIDs.size(); refIDIndex++) {
			Response showEventResponse = showEvents(refIDs, refIDIndex);
			System.out.println("Working on : " + refIDs.get(refIDIndex));
			jsonpath = new JsonPath(showEventResponse.asString());
			ruleData(jsonpath, rowIndexForRule, OUTPUT_FILE_PATH);
			actionData(jsonpath, rowIndexForRule, OUTPUT_FILE_PATH);
			rowIndexForRule++;
		}
	}

	public static void deleteEBR(ArrayList<String> ebrIDs, ArrayList<String> refIDs, String url, String path,
			int rowIndexForRule) throws InterruptedException {

		TestUtil.addStatusColumn("Status", OUTPUT_FILE_PATH, 1);
		List<String> givenEBRIds = TestUtil.getFieldValues(INPUT_FILE_PATH);
		TestUtil.cloneExcelFile(INPUT_FILE_PATH, OUTPUT_FILE_PATH);
		WebDriver driver = TestUtil.initializeWebDriver();
		TestUtil.getLogin(driver, properties);
		Thread.sleep(2000);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		for (int givenEBRIdIndex = 0; givenEBRIdIndex < givenEBRIds.size(); givenEBRIdIndex++) {
			String givenEBRId = givenEBRIds.get(givenEBRIdIndex);
			Boolean ebrFound = false;
			int index = 0;
			for (int foundEBRIdIndex = 0; foundEBRIdIndex < ebrIDs.size(); foundEBRIdIndex++) {
				String foundEBRId = ebrIDs.get(foundEBRIdIndex);
				if (givenEBRId.equalsIgnoreCase(foundEBRId)) {
					ebrFound = true;
					index = foundEBRIdIndex;
					break;
				}
			}
			if (ebrFound) {
				Thread.sleep(3000);
				System.out.println("EBR ID found " + ebrFound);
				String refId = refIDs.get(index);
				driver.get(url + path + refId);
				Thread.sleep(3000);
				String deleteXpath = "//button[@title=\"Delete\"]";
				By deleteButton = By.xpath(deleteXpath);
//				wait.until(ExpectedConditions.visibilityOfElementLocated(deleteButton));
				driver.findElement(deleteButton).click();
				driver.findElement(By.xpath("//button[@type=\"button\" and normalize-space()=\"Yes\"]")).click();
				String status = "Deleted";
				TestUtil.saveStatusInExcel(status, rowIndexForRule, OUTPUT_FILE_PATH, 1);
				// Response showEventResponse = deleteEBR(refId);
			} else {
				String status = "not found";
				TestUtil.saveStatusInExcel(status, rowIndexForRule, OUTPUT_FILE_PATH, 1);
			}
			rowIndexForRule++;
		}
	}

	public static void updateEBR(ArrayList<String> ebrIDs, ArrayList<String> refIDs, String url, String path,
			int rowIndexForRule) throws InterruptedException {

		TestUtil.addStatusColumn("Status", OUTPUT_FILE_PATH, 1);
		String updateActiveStatus = TestUtil.getInfo(properties, "updateActiveStatus");
		List<String> givenEBRIds = TestUtil.getFieldValues(INPUT_FILE_PATH);
		TestUtil.cloneExcelFile(INPUT_FILE_PATH, OUTPUT_FILE_PATH);
		WebDriver driver = TestUtil.initializeWebDriver();
		TestUtil.getLogin(driver, properties);
		Thread.sleep(2000);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		JavascriptExecutor executor = (JavascriptExecutor) driver;

		for (int givenEBRIdIndex = 0; givenEBRIdIndex < givenEBRIds.size(); givenEBRIdIndex++) {
			String givenEBRId = givenEBRIds.get(givenEBRIdIndex);
			Boolean ebrFound = false;
			int index = 0;
			for (int foundEBRIdIndex = 0; foundEBRIdIndex < ebrIDs.size(); foundEBRIdIndex++) {
				String foundEBRId = ebrIDs.get(foundEBRIdIndex);
				if (givenEBRId.equalsIgnoreCase(foundEBRId)) {
					ebrFound = true;
					index = foundEBRIdIndex;
					break;
				}
			}
			if (ebrFound) {
				String refId = refIDs.get(index);
				driver.get(url + path + refId);
				TestUtil.updateActiveStatus(driver, executor, updateActiveStatus, rowIndexForRule, OUTPUT_FILE_PATH,
						wait);
			} else {
				String status = "not found";
				TestUtil.saveStatusInExcel(status, rowIndexForRule, OUTPUT_FILE_PATH, 1);
			}
			rowIndexForRule++;
		}
	}
}