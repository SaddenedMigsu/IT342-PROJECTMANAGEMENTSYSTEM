package com.it342.projectmanagementsystem.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
    private static final String BASE_URL = "http://10.0.2.2:8080/";  // Backend server port
    private static RetrofitClient instance;
    private static Context context;
    private static Retrofit retrofit;
    private final ApiService apiService;

    private RetrofitClient(Context context) {
        RetrofitClient.context = context;
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
            Log.d("RetrofitClient", message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Log.d(TAG, "Initializing RetrofitClient with BASE_URL: " + BASE_URL);
        Log.d(TAG, "Context: " + context.getPackageName());

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                String token = prefs.getString("token", "");
                Request original = chain.request();
                
                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body());
                
                return chain.proceed(requestBuilder.build());
            })
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(Arrays.asList(
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            ))
            .build();

        try {
            Log.d(TAG, "Creating Retrofit instance...");
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "RetrofitClient initialized successfully with API service: " + apiService);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing RetrofitClient: " + e.getMessage(), e);
            throw e;
        }
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RetrofitClient must be initialized with context first");
        }
        return instance;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
    }

    public ApiService getApiService() {
        return apiService;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
} 