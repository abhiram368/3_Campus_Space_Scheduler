package com.example.tests;

import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class StaffFlowTest extends BaseTest {

    @Test
    public void testStaffComprehensiveWorkflow() {
        // 1. OPEN LOGIN PAGE AND ENTER CREDENTIALS
        performLogin("venkat@nitc.ac.in", "venkat", AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));
        
        // Wait for dashboard animation/redirection transition to fully settle
        try { Thread.sleep(1000); } catch (Exception e) {}

        // 3. PENDING REQUESTS -> APPROVE (IF ANY)
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnEscalated")); 
        
        // Wait for specific page title to confirm navigation
        waitForText(AppiumBy.id("com.example.campus_space_scheduler:id/header_title"), "Pending Requests");
        
        // Ensure the loader has finished before checking for contents
        waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
        
        // Ensure the list is actually loaded (it might take a moment even after loader is gone)
        try { Thread.sleep(500); } catch(Exception e) {}
        
        // 3. PENDING REQUESTS -> APPROVE (IF VISIBLE)
        if (isElementVisible(AppiumBy.id("com.example.campus_space_scheduler:id/requestsRecyclerView"))) {
            try {
                WebElement firstCard = driver.findElement(AppiumBy.accessibilityId("Booking Request Card"));
                firstCard.click();
                
                // 4. APPROVE WITH REMARK
                click(AppiumBy.id("com.example.campus_space_scheduler:id/btnApprove"));
                type(AppiumBy.id("com.example.campus_space_scheduler:id/et_cancel_remark"), "Staff Approved via Appium Automation");
                click(AppiumBy.id("com.example.campus_space_scheduler:id/btn_dialog_confirm"));
                
                // Staff Approval finishes directly after clicking submit
                waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
                
                // Return to list
                goBack();
            } catch (Exception e) {}
        }
        
        // Return to Dashboard - only if not already there
        if (!isElementVisible(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"))) {
            goBack(); 
        }
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 4. LIVE STATUS -> COME BACK
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnLiveStatus"));
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 5. DRAWER MENU -> PROFILE VIA HEADER -> COME BACK
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));
        click(AppiumBy.id("com.example.campus_space_scheduler:id/tvInitial")); // Clicking the "blue thing" (Initial circle)
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/header_title")));
        swipe(500, 1500, 500, 500); // Scroll Profile
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 6. APPROVAL HISTORY -> TAB SWITCHING
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnApprovalHistory"));
        String[] tabs = {"Approved", "Rejected", "Forwarded", "Cancelled", "Expired"};
        for (String tab : tabs) {
            clickByText(tab);
            waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
        }
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 7. LAB DETAILS -> EDIT NO OF STUDENTS
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnLabDetails"));
        
        // Find the 'detailStudents' row and click its edit button
        WebElement detailStudents = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/detailStudents")));
        detailStudents.findElement(AppiumBy.id("com.example.campus_space_scheduler:id/btnAction")).click(); // The Edit icon
        
        // Enter 101 and click Done
        WebElement etValue = detailStudents.findElement(AppiumBy.id("com.example.campus_space_scheduler:id/etValue"));
        etValue.clear();
        etValue.sendKeys("101");
        detailStudents.findElement(AppiumBy.id("com.example.campus_space_scheduler:id/btnDone")).click(); // The Tick icon
        
        // Handle confirmation
        clickByText("Update"); 
        waitForInvisibility(AppiumBy.xpath("//*[@text='Update']"));
        
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 8. (Omitted: My Profile not in Staff drawer)
        
        // 9. DRAWER MENU -> HELP & ABOUT
        System.out.println("Navigating to Help...");
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));
        clickDrawerItem("Help & About");
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvWeeklyBookings"));

        // 10. LOGOUT -> CONFIRM (CASE-INSENSITIVE)
        System.out.println("Trying to logout...");
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));

        // Expert Wait: Ensure Logout is visible in the drawer before clicking
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            AppiumBy.xpath("//*[contains(translate(@text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'logout')]")));

        clickDrawerItem("Logout"); 
        
        // Safety delay before final confirmation
        try { Thread.sleep(500); } catch (Exception e) {}
        
        clickByText("Confirm Logout"); 
        
        // Verify return to login page
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/btnLogin")));
    }
}
