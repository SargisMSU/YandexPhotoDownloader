package yandex;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import okhttp3.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.openqa.selenium.chrome.ChromeDriver;
import yandex.utils.FilesUtils;
import yandex.utils.HtmlParser;
import yandex.utils.Settings;
import yandex.utils.SeleniumHelper;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {

    private CloseableHttpAsyncClient client;
    private CloseableHttpClient syncClient;
    private Integer count;
    private String request;
    private ChromeDriver driver;
    private Button btn;
    private Settings params;

    public static void main(String[] args) {
        try {
            Main.launch(args);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getStackTrace());
            try {
                PrintWriter pw = new PrintWriter(new File("errorMain.txt"));
                e.printStackTrace(pw);
                pw.close();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        btn = new Button();
        btn.setText("Капча решена");
        btn.setDisable(true);
        btn.setOnAction(event -> {
            btn.setDisable(true);
            onClick();
        });
        StackPane root = new StackPane();
        root.getChildren().add(btn);
        Scene scene = new Scene(root, 350, 100);

        primaryStage.setTitle("Yandex photo downloader");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setMinWidth(350);
        primaryStage.setMinHeight(100);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(t -> {
            if (driver != null) driver.quit();
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        params = new Settings("params.txt", "black_list.txt");

        request = params.getRequest();
        count = params.getCount();

        TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy).build();

        client = HttpAsyncClients.custom()
                .setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .disableAuthCaching()
                .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(20000)
                    .setConnectionRequestTimeout(20000)
                    .build())
                .setSSLContext(sslContext).build();
        client.start();

        syncClient = HttpClients.createDefault();


        SeleniumHelper seleniumHelper = new SeleniumHelper();
        seleniumHelper.openURL(params.getUrl());
        driver = seleniumHelper.getDriver();
        btn.setDisable(false);
    }

    private void onClick() {

        new Thread(() -> {
            LinkedList<String> urlsAll = HtmlParser.parsePhotosURL(params, driver);
            driver.quit();

            String directory = FilesUtils.createDirectory(request);

            AtomicInteger k = new AtomicInteger();
            HashMap<String, Integer> errorsMap = new HashMap<>();
            HashSet<String> unavailableUrls = new HashSet<>();
            LinkedList<String> urlsTemp = new LinkedList<>();

            getNextUrls(urlsAll, urlsTemp);
            do {
                int urlTempSize = urlsTemp.size();
                CountDownLatch countDownLatch = new CountDownLatch(urlTempSize);
                for (int i = 0; i < urlTempSize; i++) {
                    String urlSetK = urlsTemp.removeFirst();
                    String currentName = directory + File.separator + "img" + k.getAndIncrement() + ".png";
                    try {
                        FilesUtils.download(errorsMap, syncClient, urlSetK, currentName, countDownLatch);
                        //FilesUtils.downloadAsync(errorsMap, client, urlSetK, currentName, countDownLatch);
                    }catch (URISyntaxException e){
                        int index = e.getIndex();
                        String urlNew = urlSetK.substring(0, index) + URLEncoder.encode(urlSetK.substring(index));
                        FilesUtils.onFailed(urlNew, errorsMap);
                        countDownLatch.countDown();
                    } catch (Exception e){
                        e.printStackTrace();
                        if (errorsMap.containsKey(urlSetK)){
                            Integer oldCount = errorsMap.get(urlSetK);
                            errorsMap.put(urlSetK, oldCount + 1);
                        }else {
                            errorsMap.put(urlSetK, 1);
                        }
                        countDownLatch.countDown();
                    }
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Map.Entry<String, Integer> entry: ((HashMap<String, Integer>)errorsMap.clone()).entrySet()){
                    if (entry.getValue() > 1){
                        errorsMap.remove(entry.getKey());
                        unavailableUrls.add(entry.getKey());
                        if (urlsAll.size() > count - 1 + unavailableUrls.size()) {
                            urlsTemp.add(urlsAll.get(count - 1 + unavailableUrls.size()));
                        }
                    }
                }
                urlsTemp.addAll(errorsMap.keySet());
                getNextUrls(urlsAll, urlsTemp);
            } while (!(errorsMap.size() == 0 && urlsTemp.size() == 0));

            if (unavailableUrls.size() > 0) {
                try {
                    Files.write(Paths.get(directory + File.separator + "unavailableUrls.txt"), unavailableUrls);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Platform.exit();
            System.exit(0);
        }).start();
    }

    private void getNextUrls(LinkedList<String> allLinks, LinkedList<String> tempLinks){
        int endIndex = Math.min(Math.min(count - tempLinks.size(), 20 - tempLinks.size()), allLinks.size());
        for (int i = 0; i < endIndex; i++) {
            tempLinks.add(allLinks.removeFirst());
        }
        count -= endIndex;
    }
}
