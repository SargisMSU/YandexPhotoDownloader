package yandex.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import sun.rmi.runtime.Log;

import java.util.LinkedList;
import java.util.List;

public class HtmlParser {

    public static LinkedList<String> parsePhotosURL(Settings params, ChromeDriver driver){
        LinkedList<String> urls = new LinkedList<>();

        //Искать только «request».
        try{
            WebElement misspell__message = driver.findElement(By.className("misspell__message"));
            List<WebElement> aElems = misspell__message.findElements(By.tagName("a"));
            for (WebElement aElem : aElems) {
                if (aElem.getText().equals(params.getRequest())) {
                    aElem.click();
                    Thread.sleep(2200);
                    break;
                }
            }
        }catch (NoSuchElementException | InterruptedException ignored){
        }
        int oldCountElements = 0;
        while (urls.size() < params.getCount() * 2) {
            List<WebElement> elements;
            if (params.getWidth() < params.getHeight() ) {
                elements = driver.findElements(By.xpath("/html/body/div[7]/div[1]/div[1]/div[1]/div/div/div/div"));
            }else {
                elements = driver.findElements(By.xpath("/html/body/div[7]/div[1]/div[1]/div[1]/div/div"));
            }
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
                        JSONObject origin = ((JSONObject) preview.get(0)).getJSONObject("origin");
                        String url = origin.getString("url");
                        if (!Settings.isFromBlackList(params.getBlackListSites(), url)) {
                            urls.add(url);
                        }
                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }
                }
                if (urls.size() == params.getCount() * 2) break;
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
