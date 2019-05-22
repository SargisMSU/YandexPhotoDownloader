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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static okhttp3.CipherSuite.*;

public class Main extends Application {

    private CloseableHttpAsyncClient client;
    private int count;
    private String request, url;
    private ChromeDriver driver;
    private Button btn;

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

        TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy).build();
        client = HttpAsyncClients.custom()
                .setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .disableAuthCaching()
                .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(30000)
                    .setConnectionRequestTimeout(30000)
                    .build())
                .setSSLContext(sslContext).build();
        client.start();

        Settings params = new Settings("params.txt");
        url = params.getUrl();
        count = params.getCount();
        request = params.getRequest();

        SeleniumHelper seleniumHelper = new SeleniumHelper();
        seleniumHelper.openURL(url);
        driver = seleniumHelper.getDriver();
        btn.setDisable(false);
    }

    /*public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, URISyntaxException, KeyManagementException {

        TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy).build();
        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .disableAuthCaching()
                .setSSLContext(sslContext).build();
        client.start();

        FilesUtils.download(new HashMap<>(), client, "https://xelk.org/content/uploads/2019/02/cristianobarcaklasikodabatawa.jpg",
                "TestFile.txt", new CountDownLatch(1));
    }*/

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

    private void onClick() {

        new Thread(() -> {
            ArrayList<String> urlsAll = HtmlParser.parsePhotosURL(driver, request, count);
            driver.quit();

            String directory = FilesUtils.createDirectory(request);

            AtomicInteger k = new AtomicInteger();
            HashMap<String, Integer> errorsMap = new HashMap<>();
            HashSet<String> unavailableUrls = new HashSet<>();
            ArrayList<String> urlsTemp = new ArrayList<>();
            for (int i = 0; i < (count > urlsAll.size() ? urlsAll.size() : count); i++) {
                urlsTemp.add(urlsAll.get(i));
            }
            do {
                CountDownLatch countDownLatch = new CountDownLatch(urlsTemp.size());
                for (int i = 0; i < urlsTemp.size(); i++) {
                    String urlSetK = urlsTemp.get(i);
                    String currentName = directory + File.separator + "img" + k.getAndIncrement() + ".png";
                    try {
                        FilesUtils.download(errorsMap, client, urlSetK, currentName, countDownLatch);
                    }catch (URISyntaxException e){
                        int index = e.getIndex();
                        String urlNew = urlSetK.substring(0, index) + URLEncoder.encode(urlSetK.substring(index));
                        if (errorsMap.containsKey(urlNew)){
                            Integer oldCount = errorsMap.get(urlNew);
                            errorsMap.put(urlNew, oldCount + 1);
                        }else {
                            errorsMap.put(urlNew, 1);
                        }
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

                urlsTemp.clear();
                for (Map.Entry<String, Integer> entry: ((HashMap<String, Integer>)errorsMap.clone()).entrySet()){
                    if (entry.getValue() > 2){
                        errorsMap.remove(entry.getKey());
                        unavailableUrls.add(entry.getKey());
                        if (urlsAll.size() > count - 1 + unavailableUrls.size()) {
                            urlsTemp.add(urlsAll.get(count - 1 + unavailableUrls.size()));
                        }
                    }
                }
                urlsTemp.addAll(errorsMap.keySet());
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
}
