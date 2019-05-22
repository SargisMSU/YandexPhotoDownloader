package yandex.utils;

import okhttp3.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class FilesUtils {

    public static void download(HashMap<String, Integer> errors, CloseableHttpAsyncClient client, String url,
                                String fileName, CountDownLatch countDownLatch) throws URISyntaxException {
        //HttpContext context = HttpClientContext.create();
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
                countDownLatch.countDown();
            }

            @Override
            public void cancelled() {
                System.out.println("Cancelled url = " + url);
            }
        });
    }

    public static String createDirectory(String dirName){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        String name = dirName + simpleDateFormat.format(new Date());
        File dir = new File(name);
        dir.mkdir();
        return name;
    }
}
