package io.github.overlordsiii.scrobbler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.overlordsiii.Main;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class PrepareScrobbleData {

	private static final List<Path> SPOTIFY_STREAMING_DATA = List.of(
		Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\MyData\\StreamingHistory0.json"),
		Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\MyData\\StreamingHistory1.json")
	);

	public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException {
		Scanner scanner = new Scanner(System.in);
		Main.refreshAPI();
		Main.LOGGER.info("Enter 1 to Parse Scrobble Data, 2 to edit it and add album data");
		switch (scanner.nextInt()) {
			case 1 -> parseScrobbleData();
			case 2 -> editScrobbleData();
			case 3 -> countSongEntries();
			case 4 -> editDurationData();
			case 5 -> getAverageListenLength();
			case 6 -> removeLocalTracks();
			case 7 -> consolidateTracks();
		}


	}




	private static void parseScrobbleData() throws IOException {
		Map<Path, List<JsonObject>> objects = new HashMap<>();

		for (Path spotifyStreamingDatum : SPOTIFY_STREAMING_DATA) {
			objects.put(spotifyStreamingDatum, new ArrayList<>());
			JsonArray array = JsonParser.parseString(Files.readString(spotifyStreamingDatum)).getAsJsonArray();
			AtomicInteger previousIndex = new AtomicInteger();

			Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1).forEach(path -> {
				if (path.getFileName().toString().startsWith("error-") && path.toString().contains(spotifyStreamingDatum.getFileName().toString())) {
					previousIndex.set(Integer.parseInt(path.getFileName().toString().substring(6, path.getFileName().toString().indexOf("_"))));
					try {
						JsonArray array1 = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
						List<JsonObject> objects1 = new ArrayList<>();
						array1.forEach(jsonElement -> {
							objects1.add(jsonElement.getAsJsonObject());
						});
						objects.replace(spotifyStreamingDatum, objects1);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});


			for (int i = previousIndex.get(); i < array.size(); i++) {
				JsonElement jsonElement = array.get(i);
				JsonObject object = jsonElement.getAsJsonObject();

				if (!object.has("msPlayed")) {
					continue;
				}

				int ms = object.get("msPlayed").getAsInt();

				if (ms < 25000) {
					continue;
				}

				object.remove("msPlayed");
				object.addProperty("duration", ms);
				List<JsonObject> objects1 = objects.get(spotifyStreamingDatum);
				objects1.add(object);
				objects.put(spotifyStreamingDatum, objects1);
			}
		}

		exportData(objects.values().stream().flatMap(Collection::stream).toList(), 1000);
	}

	private static void editScrobbleData() throws IOException {
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1).forEach(path -> {
			Main.LOGGER.info("Processing: " + path);
			if (!path.getFileName().toString().startsWith("data")) {
				return;
			}

			try {
				List<JsonObject> objectList = new ArrayList<>();
				JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
				array.forEach(jsonElement -> objectList.add(jsonElement.getAsJsonObject()));

				List<JsonObject> newObjects = objectList
					.stream()
					.map(jsonObject -> {
						if (!jsonObject.has("albumName") && !jsonObject.has("albumArtist")) {
							String q = jsonObject.get("trackName").getAsString() + " " + jsonObject.get("artistName").getAsString();
							try {
								Main.LOGGER.info("Processing: " + q);
								Track track = Main.API.searchTracks(q).build().execute().getItems()[0];
								jsonObject.addProperty("albumName", track.getAlbum().getName());
								jsonObject.addProperty("albumArtist", String.join(", ", Arrays.stream(track.getAlbum().getArtists()).map(ArtistSimplified::getName).toArray(String[]::new)));
							} catch (IOException | ParseException | SpotifyWebApiException e) {
								throw new RuntimeException(e);
							}
						}

						return jsonObject;
					})
					.toList();

				JsonArray finalArray = new JsonArray();
				newObjects.forEach(finalArray::add);

				Files.writeString(path, Main.GSON.toJson(finalArray));

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void countSongEntries() throws IOException {
		AtomicInteger size = new AtomicInteger();
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1)
			.filter(path -> path.getFileName().toString().startsWith("data"))
			.filter(path -> !Files.isDirectory(path))
			.filter(path -> path.toString().endsWith(".json"))
			.forEach(path -> {
				try {
					JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
					size.addAndGet(array.size());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

		Main.LOGGER.info("Size: " + size);
	}

	private static void getAverageListenLength() throws IOException {
		AtomicInteger integer = new AtomicInteger();
		AtomicInteger count = new AtomicInteger();
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\MyData"), 1)
			.filter(path -> path.getFileName().toString().startsWith("StreamingHistory"))
			.filter(path -> !Files.isDirectory(path))
			.filter(path -> path.toString().endsWith(".json"))
			.forEach(path -> {
				Main.LOGGER.info("Processing: " + path);
				JsonArray array;
				try {
					array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				array.forEach(jsonElement -> {
					JsonObject obj = jsonElement.getAsJsonObject();
					if (obj.has("msPlayed")) {
						integer.addAndGet(obj.get("msPlayed").getAsInt());
						count.addAndGet(1);
					}
				});
			});

		Main.LOGGER.info("Average Listen Length: " + (integer.doubleValue() / count.doubleValue()));
	}

	private static void removeLocalTracks() throws IOException {
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1)
			.filter(path -> path.getFileName().toString().startsWith("data"))
			.filter(path -> !Files.isDirectory(path))
			.filter(path -> path.toString().endsWith(".json"))
			.forEach(path -> {
				Main.LOGGER.info("Processing: " + path);
				try {
					JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
					List<JsonObject> newObjects = new ArrayList<>();
					array.forEach(jsonElement -> {
						JsonObject object = jsonElement.getAsJsonObject();
						if (object.get("trackName").getAsString().equals("Unknown Track")) {
							return;
						}
						newObjects.add(object);
					});

					JsonArray array1 = new JsonArray();
					newObjects.forEach(array1::add);
					Files.writeString(path, Main.GSON.toJson(array1));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
	}

	private static void consolidateTracks() throws IOException {
		List<JsonObject> jsonObjects = new ArrayList<>();
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1)
			.filter(path -> path.getFileName().toString().startsWith("data"))
			.filter(path -> !Files.isDirectory(path))
			.filter(path -> path.toString().endsWith(".json"))
			.forEach(path -> {
				Main.LOGGER.info("Processing: " + path);
				try {
					JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
					array.forEach(jsonElement -> {
						jsonObjects.add(jsonElement.getAsJsonObject());
					});
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

		exportData(jsonObjects, 2400);
	}

	private static void exportData(List<JsonObject> jsonObjects, int x) throws IOException {
		List<List<JsonObject>> subListX = Main.subListX(jsonObjects, x);
		for (int i = 0; i < subListX.size(); i++) {
			List<JsonObject> objects1 = subListX.get(i);
			JsonArray array = new JsonArray();
			objects1.forEach(array::add);
			Files.writeString(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData\\data" + i + ".json"), Main.GSON.toJson(array));
		}
	}

	// Convert to Hours:Minutes:Seconds.Miliseconds
	private static void editDurationData() throws IOException {
		Files.walk(Path.of("C:\\Users\\shrih\\OneDrive\\Desktop\\Spotify Data\\ParsedData"), 1)
			.filter(path -> path.getFileName().toString().startsWith("data"))
			.filter(path -> !Files.isDirectory(path))
			.filter(path -> path.toString().endsWith(".json"))
			.forEach(path -> {
				Main.LOGGER.info("Processing: " + path);
				try {
					JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
					List<JsonObject> oldObjects = new ArrayList<>();
					List<JsonObject> newObjects = new ArrayList<>();
					array.forEach(jsonElement -> oldObjects.add(jsonElement.getAsJsonObject()));
					oldObjects.forEach(jsonObject -> {
						if (jsonObject.has("duration")) {
							int ms = jsonObject.get("duration").getAsInt();
							jsonObject.remove("duration");

							int hours = new BigDecimal(ms / 3600000).round(new MathContext(3)).intValue();
							int minutes = new BigDecimal((ms % 3600000) / 60000).round(new MathContext(3)).intValue();
							int seconds = new BigDecimal(((ms % 3600000) % 60000) / 1000).round(new MathContext(3)).intValue();
							int newMs = ((ms % 3600000) % 60000) % 1000;
							String finalDuration = hours + ":" + minutes + ":" + seconds + "." + newMs;
							jsonObject.addProperty("duration", finalDuration);
						}
						newObjects.add(jsonObject);
					});
					JsonArray array1 = new JsonArray();
					newObjects.forEach(array1::add);
					Files.writeString(path, Main.GSON.toJson(array1));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
	}
}
