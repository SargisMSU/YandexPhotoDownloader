package yandex.utils;

import okhttp3.HttpUrl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    private static final String baseUrl = "https://yandex.ru/images/search";

    private String url;
    private String request;
    private int count, width, height;
    private List<String> blackListSites;

    public Settings(String paramsFileName, String blackListFile) {
        blackListSites = new ArrayList<>();
        try {
            List<String> stringList = Files.readAllLines(Paths.get(paramsFileName), StandardCharsets.UTF_8);
            count = Integer.parseInt(stringList.get(0).trim());
            request = stringList.get(1).trim();
            String[] sizes = stringList.get(2).trim().split(";");
            width = Integer.parseInt(sizes[0]);
            height = Integer.parseInt(sizes[1]);
            String color = "";
            if (stringList.size() > 3){
                color = stringList.get(3).trim();
            }
            url = getUrl(baseUrl, request, sizes[0], sizes[1], color);

            Files.readAllLines(Paths.get(blackListFile), StandardCharsets.UTF_8).stream().forEach(s -> {
                blackListSites.add(s.trim());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getUrl(String baseUrl, String text, String iw, String ih, String color) throws UnsupportedEncodingException {
        HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
        builder.addQueryParameter("text", text/*URLEncoder.encode(text, "UTF-8")*/);
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<String> getBlackListSites() {
        return blackListSites;
    }

    public static boolean isFromBlackList(List<String> blackListSites, String url){
        boolean b = false;
        for (int j = 0; j < blackListSites.size(); j++) {
            if (blackListSites.get(j).length()  > 0 && url.contains(blackListSites.get(j))){
                b = true;
                break;
            }
        }
        return b;
    }
}
