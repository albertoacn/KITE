/*
 * Copyright 2017 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.webrtc.kite;

import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;  
import org.webrtc.kite.stat.Utility;

import javax.json.Json;
import java.util.*;

public class IceConnectionTest extends KiteTest {

  private final static Logger logger = Logger.getLogger(IceConnectionTest.class.getName());

  private final static Map<String, String> expectedResultMap = new HashMap<String, String>();
  private final static String ROOM_URL = "http://localhost:3000/room/test";
  private final static int TIMEOUT = 60000;
  private final static int OVERTIME = 10000;
  private final static int INTERVAL = 1000;
  private final static int CANVAS_TOLERANCE = 15;
  private final static String RESULT_TIMEOUT = "TIME OUT";
  private final static String RESULT_SUCCESSFUL = "SUCCESSFUL";
  private final static String RESULT_FAILED = "FAILED";
  private static String alertMsg;


  static {
    expectedResultMap.put("completed", "completed");
    expectedResultMap.put("connected", "connected");
  }

  private void takeAction() throws InterruptedException{

    for (WebDriver webDriver : this.getWebDriverList()) {
      webDriver.get(ROOM_URL);
      this.click(webDriver, "connectSession");
      this.click(webDriver, "dropdown");
      this.click(webDriver, "publishCanvas");
    }
    for (WebDriver webDriver : this.getWebDriverList()) {
      this.click(webDriver, "subscribe");
    }
  }

  private void click(WebDriver webDriver, String selector) throws InterruptedException{
    WebDriverWait webDriverWait = new WebDriverWait(webDriver, 20);
    webDriverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-wd=" + selector +"]"))).click();
  }

  private boolean isCloseTo(int value, int expected){
    return value > expected - CANVAS_TOLERANCE && value < expected + CANVAS_TOLERANCE;
  }

  private final static String getPublisherVideoWidth() {
    return getVideoDims(true, true);
  }

  private final static String getPublisherVideoHeight() {
    return getVideoDims(true, false);
  }

  private final static String getSubscriberVideoWidth() {
    return getVideoDims(false, true);
  }

  private final static String getSubscriberVideoHeight() {
    return getVideoDims(false, false);
  }

  private final static String getVideoDims(boolean pub, boolean width) {
    String role = pub ? "publishers" : "subscribers";
    String dim = width ? "Width" : "Height";
    return "return OT." + role + ".find().video" + dim + "();";
  }

  private final static String getSubscriberImgData() {
    return getImgData(false);
  }

  private final static String getPublisherImgData() {
    return getImgData(true);
  }

  private final static String getImgData(boolean pub) {
    String role = pub ? "publishers" : "subscribers";
    return  "video = OT." + role + ".find().videoElement();" +
            "tmpCanvas = OT.$.createElement('canvas');" +
            "tmpCtx = tmpCanvas.getContext('2d');" +
            "tmpCtx.drawImage(video, 0, 0, tmpCanvas.width, tmpCanvas.height);" +
            "imgData = tmpCtx.getImageData(0, 0, 1, 1);" +
            "return imgData.data[0]";
  }

  private int[] getResults(WebDriver webDriver){
    int videoPubWidth =
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getPublisherVideoWidth())));
    int videoPubHeight =
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getPublisherVideoHeight())));
    int pubImgData =
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getPublisherImgData())));
      
    int videoSubWidth =
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getSubscriberVideoWidth())));
    int videoSubHeight =
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getSubscriberVideoHeight())));
    int subImgData = 
           Integer.parseInt(String.valueOf(((JavascriptExecutor) webDriver).executeScript(this.getSubscriberImgData())));

    int[] results = {videoPubWidth, videoPubHeight, videoSubWidth, videoSubHeight, pubImgData, subImgData};

    return results;
  }

  private boolean validateResults(Map<String, int[]> browsers) {
    String chrome = "chrome";
    String firefox = "firefox";
    for( int i = 0; i < 4; i++){
      if (browsers.get(chrome)[i] <= 0 || browsers.get(firefox)[i] <= 0)
        return false;
    }
    boolean result = Float.compare(browsers.get(chrome)[0] / browsers.get(chrome)[1], browsers.get(firefox)[0] / browsers.get(firefox)[1]) == 0;
    result = result && isCloseTo(browsers.get(chrome)[4], browsers.get(firefox)[5]);
    result = result && isCloseTo(browsers.get(chrome)[5], browsers.get(firefox)[4]);
    return result;
  }

  @Override
  public Object testScript() throws Exception {
    
    String result = RESULT_TIMEOUT;
    Map<String, Object> resultMap = new HashMap<String, Object>();
    Map<String, int[]> browsers = new HashMap<String, int[]>();
    
    this.takeAction();
    //TO change for implicit wait
    Thread.sleep(10000);
    for (WebDriver webDriver : this.getWebDriverList()) {
      String browserName = String.valueOf(((RemoteWebDriver) webDriver).getCapabilities().getBrowserName().toLowerCase());
      int[] stats = this.getResults(webDriver);
      browsers.put(browserName, stats);
    }
    boolean res = this.validateResults(browsers);

    browsers.clear();

    Thread.sleep(5001);
    for (WebDriver webDriver : this.getWebDriverList()) {
      String browserName = String.valueOf(((RemoteWebDriver) webDriver).getCapabilities().getBrowserName().toLowerCase());
      int[] stats = this.getResults(webDriver);
      browsers.put(browserName, stats);
    } 

    result = res && this.validateResults(browsers) ? RESULT_SUCCESSFUL : RESULT_FAILED;

    resultMap.put("result", result);
    return Utility.developResult(resultMap, this.getWebDriverList().size()).toString();
  }

}
