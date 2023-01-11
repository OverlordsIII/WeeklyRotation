package io.github.overlordsiii.util;

import com.google.gson.JsonObject;
import io.github.overlordsiii.Main;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Request {
    private final static HttpClient CLIENT = HttpClient.newHttpClient();

    private final static String BACKEND_PATH = Main.PRIVATE_CONFIG.getConfigOption("genius-api-path");


    public static JsonObject makeRequest(String path, Method method, JsonObject body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest
                .newBuilder()
                .method(method.name(), JsonUtils.toBody(body))
                .header("Content-Type", "application/json")
                .uri(URI.create(path))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        Main.LOGGER.info("Made " + method.name() + " request to " + BACKEND_PATH + path + " with body: " + JsonUtils.objToString(body));

        JsonObject responseObj = JsonUtils.toJsonObj(response.body());
        // uncomment when u need to debug
        //  Main.LOGGER.info("Response: " + JsonUtils.objToString(responseObj));

        return responseObj;
    }
}
