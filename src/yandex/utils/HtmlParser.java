package yandex.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class HtmlParser {

    public static LinkedList<String> parsePhotosURL(ChromeDriver driver, String request, int count, boolean mobileResolution){
        LinkedList<String> urls = new LinkedList<>();

        //Искать только «request».
        try{
            WebElement misspell__message = driver.findElement(By.className("misspell__message"));
            List<WebElement> aElems = misspell__message.findElements(By.tagName("a"));
            for (WebElement aElem : aElems) {
                if (aElem.getText().equals(request)) {
                    aElem.click();
                    Thread.sleep(2200);
                    break;
                }
            }
        }catch (NoSuchElementException | InterruptedException ignored){
        }
                                                                       //html/body/div[7]/div[1]/div[1]/div[1]/div/div/div[4]
        int oldCountElements = 0;                                     //html/body/div[7]/div[1]/div[1]/div[1]/div/div
        while (urls.size() < count * 2) {                             //html/body/div[7]/div[1]/div[1]/div[1]/div/div
            List<WebElement> elements;
            if (mobileResolution) {
                elements = driver.findElements(By.xpath("/html/body/div[7]/div[1]/div[1]/div[1]/div/div/div/div"));
            }else {
                elements = driver.findElements(By.xpath("/html/body/div[7]/div[1]/div[1]/div[1]/div/div"));
            }
            System.out.println("elements.size() = " + elements.size());
            int startIndex = urls.size();
            for (int i = startIndex; i < elements.size(); i++) {
                WebElement element = elements.get(i);
                (driver).executeScript("arguments[0].scrollIntoView(true);", element);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //
                if (element.getAttribute("class").equals("justifier__cols")){
                    element = element.findElement(By.xpath("./div[1]"));
                }
                String json = element.getAttribute("data-bem");
                 if (json != null && json.startsWith("{")) {
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject serpItem = jsonObject.getJSONObject("serp-item");
                        JSONArray preview = serpItem.getJSONArray("preview");
                        String url = ((JSONObject) preview.get(0)).getString("url");
                        urls.add(url);
                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }
                }
                if (urls.size() == count * 2) break;
            }

            //если после скроллинга не появляются новые фотографии, то их больше нет.
            if (oldCountElements == elements.size()){
                break;
            }
            oldCountElements = elements.size();
        }
        return urls;
    }
}
