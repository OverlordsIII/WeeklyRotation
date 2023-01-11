package io.github.overlordsiii.genius;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

public class GeniusUtils {
	public static List<String> getArtistNamesFromArtistBodies(List<JsonObject> objects) {
		List<String> strings = new ArrayList<>();
		objects.forEach(jsonObject -> {
			strings.add(jsonObject.get("response").getAsJsonObject().get("artist").getAsJsonObject().get("name").getAsString());
		});

		return strings;
	}
}
