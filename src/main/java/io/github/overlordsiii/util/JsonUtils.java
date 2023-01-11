package io.github.overlordsiii.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.net.http.HttpRequest;

public class JsonUtils {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static String objToString(JsonObject object) {
        return GSON.toJson(object);
    }

    public static HttpRequest.BodyPublisher toBody(JsonObject object) {
        if (object == null) {
            return HttpRequest.BodyPublishers.noBody();
        }

        return HttpRequest.BodyPublishers.ofString(objToString(object));
    }

    public static JsonObject toJsonObj(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }
}
