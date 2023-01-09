package io.github.overlordsiii.genius;

import java.io.IOException;
import java.time.chrono.IsoEra;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.overlordsiii.Main;

import static io.github.overlordsiii.genius.GeniusAuthentication.*;

public class GeniusRequests {

	public static Response exampleRequest() throws IOException, ExecutionException, InterruptedException {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.genius.com/songs/378195");
		GeniusAuthentication.signRequest(request);
		return SERVICE.execute(request);
	}

	public static String beautifyResponse(Response response) throws IOException {
		String body = response.getBody();

		JsonElement element = Main.GSON.fromJson(body, JsonElement.class);

		return Main.GSON.toJson(element);
	}
}
