package io.github.overlordsiii.genius;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overlordsiii.Main;
import io.github.overlordsiii.util.Method;
import io.github.overlordsiii.util.Request;


//See https://docs.genius.com/
public class GeniusRequests {

	private static final String BASE_SEARCH_STR = "https://genius.com/api/search?q=";

	private static final String BASE_SONG_ID_STR = "https://genius.com/api/songs/";

	public static boolean isArtistOnSong(String song, String songArtist, String[] artist) throws IOException, InterruptedException {

		String encodedName = URLEncoder.encode(song + " " + songArtist, StandardCharsets.UTF_8);


		JsonObject object = Request.makeRequest(BASE_SEARCH_STR + encodedName, Method.GET, null);
		if (object == null) {
			return false;
		}

		checkRequest(object);
		JsonArray hitsArray = object.get("response").getAsJsonObject().getAsJsonArray("hits");

		if (hitsArray.size() < 1) {
			return false;
		}
		int index = 0;

		JsonObject result = hitsArray.get(index).getAsJsonObject().get("result").getAsJsonObject();
		String artists = result.get("artist_names").getAsString();

		while (artists.contains("Spotify")) {
			index++;
			if (index >= hitsArray.size()) {
				String encodedSong = URLEncoder.encode(song, StandardCharsets.UTF_8);
				JsonObject obj = Request.makeRequest(BASE_SEARCH_STR + encodedSong,Method.GET, null);
				checkRequest(obj);
				artists = obj.get("response").getAsJsonObject().getAsJsonArray("hits").get(0).getAsJsonObject().get("result").getAsJsonObject().get("artist_names").getAsString();
				break;
			}
			JsonObject result2 = hitsArray.get(index).getAsJsonObject().get("result").getAsJsonObject();
			artists = result2.get("artist_names").getAsString();
		}

		boolean bl = true;

		for (String s : artist) {
			if (!artists.contains(s)) {
				bl = false;
				break;
			}
		}
		return bl;
	}

	public static boolean isProducerOnSong(String song, String songArtist, String producerName) throws IOException, InterruptedException {

		String encodedName = URLEncoder.encode(song + " " + songArtist, StandardCharsets.UTF_8);


		JsonObject object = Request.makeRequest(BASE_SEARCH_STR + encodedName, Method.GET, null);
		if (object == null) {
			return false;
		}

		checkRequest(object);
		JsonArray hitsArray = object.get("response").getAsJsonObject().getAsJsonArray("hits");

		if (hitsArray.size() < 1) {
			return false;
		}
		int index = 0;

		JsonObject result = hitsArray.get(index).getAsJsonObject().get("result").getAsJsonObject();
		String artists = result.get("artist_names").getAsString();

		while (artists.contains("Spotify")) {
			index++;
			if (index >= hitsArray.size()) {
				String encodedSong = URLEncoder.encode(song, StandardCharsets.UTF_8);
				JsonObject obj = Request.makeRequest(BASE_SEARCH_STR + encodedSong, Method.GET, null);
				checkRequest(obj);
				result = obj.get("response").getAsJsonObject().getAsJsonArray("hits").get(0).getAsJsonObject().get("result").getAsJsonObject();
				break;
			}
			result = hitsArray.get(index).getAsJsonObject().get("result").getAsJsonObject();
			artists = result.get("artist_names").getAsString();
		}

		int id = result.get("id").getAsInt(); //song id

		JsonObject response = Request.makeRequest(BASE_SONG_ID_STR + id, Method.GET, null);
		if (response == null) {
			return false;
		}

		checkRequest(response);

		StringBuilder producers = new StringBuilder();

		response.getAsJsonObject("song").getAsJsonArray("producer_artists").forEach(jsonElement -> {
			JsonObject object1 = jsonElement.getAsJsonObject();
			String name = object1.get("name").getAsString();
			producers.append(name).append(", ");
		});

		return producers.toString().contains(producerName);
	}

	// could be name of song or name of artist
	public static List<JsonObject> getArtistsFromName(String name, boolean song) throws IOException, InterruptedException {

		List<JsonObject> objects = new ArrayList<>();

		String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);


		JsonObject object = Request.makeRequest(BASE_SEARCH_STR + encodedName, Method.GET, null);
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
}
