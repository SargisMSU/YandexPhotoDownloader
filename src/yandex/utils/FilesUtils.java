package yandex.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class FilesUtils {

    public static void downloadAsync(HashMap<String, Integer> errors, CloseableHttpAsyncClient client, String url,
                                String fileName, CountDownLatch countDownLatch) throws URISyntaxException {
        client.execute(new HttpGet(new URI(url)), new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    try {
                        Files.copy(httpResponse.getEntity().getContent(), Paths.get(fileName));
                        errors.remove(url);
                    } catch (IOException e) {
                        try {
                            Files.delete(Paths.get(fileName));
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }else {
                    System.out.println("Unavailable url: " + url);
                    if (errors.containsKey(url)){
                        Integer oldCount = errors.get(url);
                        errors.put(url, oldCount + 1);
                    }else {
                        errors.put(url, 1);
                    }
                }
                System.out.println("countDown " + fileName);
                countDownLatch.countDown();
            }

            @Override
            public void failed(Exception e) {
                System.out.println("Failed url: " + url);
                e.printStackTrace();
                if (errors.containsKey(url)){
                    Integer oldCount = errors.get(url);
                    errors.put(url, oldCount + 1);
                }else {
                    errors.put(url, 1);
                }
                System.out.println("countDown " + fileName);
                countDownLatch.countDown();
            }

            @Override
            public void cancelled() {
                System.out.println("Cancelled url = " + url);
            }
        });
    }

    public static void download(HashMap<String, Integer> errors, CloseableHttpClient client, String url,
                                String fileName, CountDownLatch countDownLatch) throws URISyntaxException, IOException {
        CloseableHttpResponse response = client.execute(new HttpGet(new URI(url)));
        if (response.getStatusLine().getStatusCode() == 200) {
            try {
                Files.copy(response.getEntity().getContent(), Paths.get(fileName));
                errors.remove(url);
            } catch (IOException e) {
                try {
                    Files.delete(Paths.get(fileName));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        } else {
            System.out.println("Unavailable url: " + url);
            if (errors.containsKey(url)) {
                Integer oldCount = errors.get(url);
                errors.put(url, oldCount + 1);
            } else {
                errors.put(url, 1);
            }
        }
        System.out.println("countDown " + fileName);
        countDownLatch.countDown();
    }

    public static String createDirectory(String dirName){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        String name = dirName + simpleDateFormat.format(new Date());
        File dir = new File(name);
        dir.mkdir();
        return name;
    }
}
