package com.example.tests;

import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class HodFlowTest extends BaseTest {

    @Test
    public void testHodApproveRequest() {
        // 1. LOGIN AS HOD
        performLogin("harsha.nair@nitc.ac.in", "F870345CS", AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));
        
        // Wait for dashboard animation/redirection transition to fully settle
        try { Thread.sleep(1000); } catch (Exception e) {}

        // 2. NAVIGATE TO ESCALATED REQUESTS (IF ANY)
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnEscalatedRequests")); 
        
        // Wait for specific page title to confirm navigation
        waitForText(AppiumBy.id("com.example.campus_space_scheduler:id/header_title"), "Escalated Requests");
        
        // Ensure the loader has finished before checking for contents
        waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
        
        // Ensure the list is actually loaded (slight pause for animation)
        try { Thread.sleep(500); } catch(Exception e) {}
        
        // 3. SELECT FIRST REQUEST IF VISIBLE (NOT EMPTY)
        if (isElementVisible(AppiumBy.id("com.example.campus_space_scheduler:id/requestsRecyclerView"))) {
            try {
                WebElement firstCard = driver.findElement(AppiumBy.accessibilityId("Booking Request Card"));
                firstCard.click();

                // 4. APPROVE WITH REMARK
                click(AppiumBy.id("com.example.campus_space_scheduler:id/btnApprove"));
                type(AppiumBy.id("com.example.campus_space_scheduler:id/et_cancel_remark"), "HOD Approved via Appium Automation");
                click(AppiumBy.id("com.example.campus_space_scheduler:id/btn_dialog_confirm"));
                
                // HOD Approval finishes directly after clicking submit
                waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
                
                // Return to list
                goBack();
            } catch (Exception e) {}
        }
        
        // Return to Dashboard - only if not already there
        if (!isElementVisible(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"))) {
            goBack(); 
        }
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));

        // 5. NOTIFICATIONS -> DELETE ONE -> COME BACK
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));
        clickDrawerItem("Notifications");
        waitForText(AppiumBy.id("com.example.campus_space_scheduler:id/header_title"), "Notifications");
        
        if (isElementPresent(AppiumBy.id("com.example.campus_space_scheduler:id/notifications_recycler"))) {
            WebElement recycler = driver.findElement(By.id("com.example.campus_space_scheduler:id/notifications_recycler"));
            List<WebElement> items = recycler.findElements(By.className("android.view.ViewGroup"));
            if (!items.isEmpty()) {
                // Long click to enter selection mode
                longClick(items.get(0));
                
                // Click Delete button (e.g., "Delete (1)")
                click(AppiumBy.id("com.example.campus_space_scheduler:id/btn_delete_selected"));
                
                // Confirm in dialog
                clickByText("Delete");
                
                // Wait for toast or refresh
                try { Thread.sleep(1000); } catch(Exception e) {}
            }
        }
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));

        // 6. LIVE STATUS -> COME BACK
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnLiveStatus"));
        waitForText(AppiumBy.id("com.example.campus_space_scheduler:id/header_title"), "Live Status");
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));

        // 6. DRAWER MENU -> PROFILE VIA HEADER -> COME BACK
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));
        click(AppiumBy.id("com.example.campus_space_scheduler:id/tvInitial")); // Clicking the "blue thing" (Initial circle)
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/header_title")));
        swipe(500, 1500, 500, 500); // Scroll Profile
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));

        // 7. APPROVAL HISTORY -> TAB SWITCHING
        click(AppiumBy.id("com.example.campus_space_scheduler:id/btnApprovalHistory"));
        waitForText(AppiumBy.id("com.example.campus_space_scheduler:id/header_title"), "Approval History");
        
        String[] tabs = {"Approved", "Rejected", "Cancelled", "Expired"};
        for (String tab : tabs) {
            clickByText(tab);
            waitForInvisibility(AppiumBy.id("com.example.campus_space_scheduler:id/progressBar"));
            try { Thread.sleep(300); } catch(Exception e) {}
        }
        goBack();
        waitForDashboard(AppiumBy.id("com.example.campus_space_scheduler:id/tvTotalBookings"));

        // 8. LOGOUT -> CONFIRM (CASE-INSENSITIVE)
        click(AppiumBy.id("com.example.campus_space_scheduler:id/menuIcon"));
        
        // Use the specialized drawer clicker (which handles scrolling)
        clickDrawerItem("Logout"); 
        
        // Click the positive button in the Material Dialog
        clickByText("Confirm Logout"); 
        
        // Verify return to login page
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.example.campus_space_scheduler:id/btnLogin")));
    }
}
