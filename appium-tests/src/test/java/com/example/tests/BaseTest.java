package com.example.tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                // For physical devices, deviceName is often ignored but required by some Appium versions.
                // UDID is recommended if multiple devices are connected.
                .setDeviceName("OnePlus CPH2467") 
                .setAppPackage("com.example.campus_space_scheduler")
                .setAppActivity("com.example.campus_space_scheduler.SplashActivity")
                .setNoReset(true)
                .setFullReset(false)
                .setUdid("d2f1eed2"); 

        // Ensure Appium server is running at this URL before starting tests
        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(text);
    }

    protected void swipe(int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(swipe));
    }

    protected void waitForInvisibility(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void clickByText(String text) {
        try {
            // 1. Try Exact Match First (using normalize-space)
            String exactXpath = "//*[normalize-space(translate(@text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) = '" + text.toLowerCase().trim() + "']";
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(exactXpath))).click();
        } catch (Exception e) {
            // 2. Fallback to Partial Match only if exact fails
            String lowerText = text.toLowerCase();
            String partialXpath = "//*[contains(translate(@text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lowerText + "')]";
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(partialXpath))).click();
        }
    }

    protected void clickDrawerItem(String text) {
        // 1. Hide Keyboard first if present
        try { driver.hideKeyboard(); } catch (Exception e) {}
        
        // 2. Wait for the Drawer to actually be visible. 
        wait.until(ExpectedConditions.or(
            ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/navigationView")),
            ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/nav_view"))
        ));
        
        // Brief pause for animation to settle
        try { Thread.sleep(800); } catch (Exception e) {}
        
        // 3. ID-First Targeting (The most reliable way)
        String idSuffix = null;
        String lowerText = text.toLowerCase();
        if (lowerText.contains("profile")) idSuffix = "nav_profile";
        else if (lowerText.contains("logout")) idSuffix = "nav_logout";
        else if (lowerText.contains("help")) idSuffix = "nav_help";
        else if (lowerText.contains("notifications")) idSuffix = "nav_notifications";
        else if (lowerText.contains("history")) idSuffix = "nav_history";
        
        if (idSuffix != null) {
            try {
                driver.findElement(AppiumBy.id("com.example.campus_space_scheduler:id/" + idSuffix)).click();
                return;
            } catch (Exception e) {
                // If ID click fails, fall through to text loop
            }
        }

        // 4. User-suggested Loop + Swipe fallback
        for (int i = 0; i < 5; i++) {
            try {
                clickByText(text);
                return;
            } catch (Exception e) {
                swipe(200, 1500, 200, 800); 
                try { Thread.sleep(300); } catch (InterruptedException ex) {}
            }
        }
        
        clickByText(text);
    }

    protected void longClick(WebElement element) {
        Point location = element.getLocation();
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence longClick = new Sequence(finger, 1);
        longClick.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), location.x, location.y));
        longClick.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        longClick.addAction(finger.createPointerMove(Duration.ofSeconds(2), PointerInput.Origin.viewport(), location.x, location.y));
        longClick.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(longClick));
    }

    protected boolean isElementPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementVisible(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void goBack() {
        try {
            // Try to click the specific back button in our toolbars
            click(AppiumBy.id("com.example.campus_space_scheduler:id/btnBack"));
        } catch (Exception e) {
            // Fallback to system back navigation
            driver.navigate().back();
        }
        // Wait for transition to complete
        try { Thread.sleep(1000); } catch(Exception e) {}
    }

    protected void scrollTo(String text) {
        driver.findElement(AppiumBy.androidUIAutomator(
            "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(" +
            "new UiSelector().textContains(\"" + text + "\"))"
        ));
    }

    protected void waitForDashboard(By indicator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(indicator));
        // Additional settle time for card interactions
        try { Thread.sleep(800); } catch(Exception e) {}
    }

    protected void waitForText(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    protected void performLogin(String user, String pass, By dashboardIndicator) {
        type(AppiumBy.id("com.example.campus_space_scheduler:id/etUser"), user);
        type(AppiumBy.id("com.example.campus_space_scheduler:id/etPass"), pass);
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnLogin"));

        try {
            // Wait for dashboard with a shorter timeout first to check for quick failure
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(dashboardIndicator));
        } catch (Exception e) {
            // If dashboard doesn't appear, check if we are still on Login page with an error
            if (isElementPresent(AppiumBy.id("com.example.campus_space_scheduler:id/btnLogin"))) {
                throw new RuntimeException("LOGIN FAILED: Still on Login screen. Check credentials for user: " + user);
            }
            throw e; 
        }
    }
}
