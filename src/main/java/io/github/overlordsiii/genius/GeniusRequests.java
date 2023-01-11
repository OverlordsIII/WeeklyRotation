package io.github.overlordsiii.genius;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.chrono.IsoEra;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overlordsiii.Main;
import io.github.overlordsiii.util.Method;
import io.github.overlordsiii.util.Request;

public class GeniusRequests {

	public static JsonObject getArtistFromName(String name) throws IOException, InterruptedException, URISyntaxException {

		JsonObject object = Request.makeRequest("https://genius.com/api/search?q=" + String.join("+", name.split("\\s+")), Method.GET, null);
		return object;
	}

	public static String beautifyResponse(Response response) throws IOException {
		String body = response.getBody();

		JsonElement element = Main.GSON.fromJson(body, JsonElement.class);

		return Main.GSON.toJson(element);
	}
}
