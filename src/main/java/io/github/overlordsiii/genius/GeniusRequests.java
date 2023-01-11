package io.github.overlordsiii.genius;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.chrono.IsoEra;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overlordsiii.Main;
import io.github.overlordsiii.util.Method;
import io.github.overlordsiii.util.Request;


//See https://docs.genius.com/
public class GeniusRequests {

	// could be name of song or name of
	public static List<JsonObject> getArtistsFromName(String name, boolean song) throws IOException, InterruptedException {

		List<JsonObject> objects = new ArrayList<>();

		JsonObject object = Request.makeRequest("https://genius.com/api/search?q=\"" + String.join("+", name.split("\\s+")) + "\"", Method.GET, null);
		if (object == null) {
			return objects;
		}

		checkRequest(object);
		JsonArray hitsArray = object.get("response").getAsJsonObject().getAsJsonArray("hits");

		if (hitsArray.size() < 1) {
			return objects;
		}

		JsonObject result = hitsArray.get(0).getAsJsonObject().get("result").getAsJsonObject();
		int primaryArtist = result.get("primary_artist").getAsJsonObject().get("id").getAsInt();

		JsonObject artistResponse = Request.makeRequest("https://genius.com/api/artists/" + primaryArtist, Method.GET, null);
		checkRequest(artistResponse);

		objects.add(artistResponse);
		if (song) {
			result.get("featured_artists").getAsJsonArray().forEach(jsonElement -> {
				JsonObject object1 = jsonElement.getAsJsonObject();
				int artistId = object1.get("id").getAsInt();
				try {
					JsonObject featuredArtistResponse = Request.makeRequest("https://genius.com/api/artists/" + artistId, Method.GET, null);
					checkRequest(featuredArtistResponse);
					objects.add(featuredArtistResponse);
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		}

		return objects;
	}


	private static void checkRequest(JsonObject response) {
		JsonObject meta = response.get("meta").getAsJsonObject();
		int status = meta.get("status").getAsInt();
		if (status != 200) {
			throw new RuntimeException("Error while processing request!: " + meta.get("message").getAsString());
		}
	}

	public static String beautifyResponse(Response response) throws IOException {
		String body = response.getBody();

		JsonElement element = Main.GSON.fromJson(body, JsonElement.class);

		return Main.GSON.toJson(element);
	}
}
