package com.lcchu.shushu;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public class OkHttpSingleton {
    private static OkHttpClient client;

    private OkHttpSingleton() {} // 私有建構子，防止外部直接實例化

    public static synchronized OkHttpClient getInstance() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 設定連線池
                    .retryOnConnectionFailure(true) // 自動重試
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}