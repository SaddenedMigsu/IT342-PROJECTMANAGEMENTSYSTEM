package com.it342.projectmanagementsystem.api;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionSpec;
import java.util.Arrays;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    // In Android emulator, 10.0.2.2 is the special IP that maps to the host machine's localhost
    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, "OkHttp: " + message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                
                // Add headers for CORS and content type
                okhttp3.Request request = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Origin", "http://10.0.2.2:8080")
                    .method(original.method(), original.body())
                    .build();

                Log.d(TAG, "Sending request to: " + request.url());
                return chain.proceed(request);
            })
            .connectTimeout(30, TimeUnit.SECONDS)  // Increased timeout
            .readTimeout(30, TimeUnit.SECONDS)     // Increased timeout
            .writeTimeout(30, TimeUnit.SECONDS)    // Increased timeout
            .retryOnConnectionFailure(true)
            .proxy(Proxy.NO_PROXY)  // Try without proxy
            .connectionSpecs(Arrays.asList(
                ConnectionSpec.CLEARTEXT,  // Try cleartext first
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            ))
            .build();

        try {
            Log.d(TAG, "Creating Retrofit instance with BASE_URL: " + BASE_URL);
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "RetrofitClient initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing RetrofitClient: " + e.getMessage(), e);
            throw e;
        }
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            try {
                instance = new RetrofitClient();
            } catch (Exception e) {
                Log.e(TAG, "Error creating RetrofitClient instance: " + e.getMessage(), e);
                throw e;
            }
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
} 