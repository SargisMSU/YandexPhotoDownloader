package yandex.utils;

import okhttp3.HttpUrl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Settings {

    private static final String baseUrl = "https://yandex.ru/images/search";

    private String url;
    private String request;
    private int count;

    public Settings(String fileName) {
        try {
            List<String> stringList = Files.readAllLines(Paths.get(fileName));
            count = Integer.parseInt(stringList.get(0).trim());
            request = stringList.get(1).trim();
            String[] sizes = stringList.get(2).trim().split(";");
            String color = "";
            if (stringList.size() > 3){
                color = stringList.get(3).trim();
            }
            url = getUrl(baseUrl, request, sizes[0], sizes[1], color);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getUrl(String baseUrl, String text, String iw, String ih, String color){
        HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
        builder.addQueryParameter("text", text);
        builder.addQueryParameter("isize", "eq");
        builder.addQueryParameter("iw",  iw);
        builder.addQueryParameter("ih",  ih);
        if (!color.equals("")) builder.addQueryParameter("icolor",  color);
        return builder.build().toString();
    }

    public String getUrl() {
        return url;
    }

    public int getCount() {
        return count;
    }

    public String getRequest() {
        return request;
    }
}
