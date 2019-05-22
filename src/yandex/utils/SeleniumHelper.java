package yandex.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumHelper {
    private ChromeDriver driver;
    private JavascriptExecutor executor;
    private ChromeOptions options;

    public SeleniumHelper() {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        options = new ChromeOptions();

        options.addArguments("disable-infobars");
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);

        executor = (JavascriptExecutor) driver;
    }

    public void openURL(String url){
        driver.get(url);
    }

    public ChromeDriver getDriver() {
        return driver;
    }

    public JavascriptExecutor getExecutor() {
        return executor;
    }

    public ChromeOptions getOptions() {
        return options;
    }

    public static WebElement getElementBy(WebDriver driver, By by){
        WebDriverWait wait = new WebDriverWait(driver, 15);
        WebElement findedElement = wait.until(
                ExpectedConditions.visibilityOfElementLocated(by));
        return findedElement;
    }
}
