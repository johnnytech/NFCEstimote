package com.estimote.nfcstamps;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Johnny on 13/2/18.
 */

public class NetworkService {

    private static final long CONNECT_TIMEOUT = 20000;   // 2 seconds
    private static final long READ_TIMEOUT = 20000;      // 2 seconds
    private static OkHttpClient okHttpClient = null;
    private static final String AUTH_URL = "https://tb-api.centech-poc.com/auth-token";
    private static final String BEACONS_URL = "https://tb-api.centech-poc.com/admin/beacons";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NAME = "beaconName";
    private static final String KEY_ID = "beaconId";
    private static final String EMAIL = "trackback.admin@centrality.tech";
    private static final String PASSWORD = "produce";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Method to build and return an OkHttpClient so we can set/get
     * headers quickly and efficiently.
     *
     * @return OkHttpClient
     */
    public OkHttpClient buildClient() {
        if (okHttpClient != null) return okHttpClient;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // Logging interceptor
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        //Timber.tag("OkHttp").d(message);
                        System.out.println("OkHttp: " + message);
                    }
                });
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpClientBuilder.addInterceptor(httpLoggingInterceptor);

        /*
        // custom interceptor for adding header and NetworkMonitor sliding window
        okHttpClientBuilder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                // Add whatever we want to our request headers.
                Request request = chain.request().newBuilder().addHeader("Accept", "application/json").build();
                Response response;
                try {
                    response = chain.proceed(request);
                } catch (SocketTimeoutException | UnknownHostException e) {
                    e.printStackTrace();
                    throw new IOException(e);
                }
                return response;
            }
        });*/

        return okHttpClientBuilder.build();
    }

    String jsonContent() {
        /*
        return "{"
                + "'email':'trackback.admin@centrality.tech',"
                + "'password':'produce'"
                + "}";
        */

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_EMAIL, EMAIL);
            jsonObject.put(KEY_PASSWORD, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    private Request.Builder buildRequest(URL url, String json) {
        RequestBody body = RequestBody.create(JSON, json);
        System.out.println("RequestBody: type=" + body.contentType());

        return new Request.Builder()
                .header("Content-Type", "application/json")
                .post(body)
                .url(url);
    }

    private Request.Builder buildRequest(URL url, String json, String credential) {
        return buildRequest(url, json).header("Authorization", credential);
    }

    private URL buildURL(Uri builtUrl) {
        if (builtUrl == null) return null;
        try {
            String urlStr = builtUrl.toString();
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private URL buildURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getData(Request request) {
        System.out.println("Request: " + request.toString());
        OkHttpClient client = buildClient();
        try {
            System.out.println("Request execute ...");
            Response response = client.newCall(request).execute();
            System.out.println("Response: " + response.toString());
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getString(String endpoint, String username, String password) {
        System.out.println("NetworkService: getString by username and password from " + endpoint);
        String credentials = username + ":" + password;
        final String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Request request = buildRequest(buildURL(endpoint), basicAuth).build();
        return getData(request);
    }

    public String getString(String endpoint, String token) {
        System.out.println("NetworkService: getString by Bearer token from " + endpoint);
        String credentials = "Bearer " + token;
        Request request = buildRequest(buildURL(endpoint), credentials).build();
        return getData(request);
    }

    public String getToken() {
        System.out.println("NetworkService: login()");
        Uri uri = Uri.parse(AUTH_URL)
                .buildUpon()
                //.appendQueryParameter(KEY_EMAIL, EMAIL)
                //.appendQueryParameter(KEY_PASSWORD, PASSWORD)
                .build();
        URL url = buildURL(uri);

        System.out.println("NetworkService: built request url: " + url.toString());

        Request request = buildRequest(url, jsonContent()).build();

        String response = getData(request);

        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getString(KEY_TOKEN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String refreshToken(String token) {
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .get()
                .url(AUTH_URL)
                .build();
        String response = getData(request);

        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getString(KEY_TOKEN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Request changeNameRequest(String name, String id, String token) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_NAME, name);
            jsonObject.put(KEY_ID, id);
            //jsonObject.put(name, id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buildRequest(buildURL(BEACONS_URL), jsonObject.toString(), token).build();
    }
}
