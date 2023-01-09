package io.github.overlordsiii;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.overlordsiii.config.PropertiesHandler;
import io.github.overlordsiii.genius.GeniusAuthentication;
import io.github.overlordsiii.genius.GeniusRequests;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.enums.ReleaseDatePrecision;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.special.AlbumSimplifiedSpecial;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.AbstractDataPagingRequest;
import se.michaelthelin.spotify.requests.data.browse.GetRecommendationsRequest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class Main {

    public static Logger LOGGER = LoggerFactory.getLogger("Weekly Rotation");

    public static PropertiesHandler PRIVATE_CONFIG = PropertiesHandler
        .builder()
        .addConfigOption("client-id", "")
        .addConfigOption("client-secret", "")
        .addConfigOption("auth-code", "")
        .addConfigOption("access-token", "")
        .addConfigOption("refresh-token", "")
        .addConfigOption("redirect-url", "https://example.com/spotify-redirect")
        .addConfigOption("genius-client-id", "")
        .addConfigOption("genius-client-secret", "")
        .addConfigOption("genius-access-token", "")
        .addConfigOption("genius-access-token-type", "")
        .addConfigOption("genius-auth-code", "")
        .setFileName("private-config.properties")
        .build();

    public static PropertiesHandler GENERAL_CONFIG = PropertiesHandler
        .builder()
        .addConfigOption("recent-album-month-limit", 2)
        .addConfigOption("recent-songs-limit", 7)
        .addConfigOption("weekly-rotation-song-limit", 25)
        .addConfigOption("weekly-rotation-playlist-public", true)
        .setFileName("general-config.properties")
        .build();


    public static SpotifyApi API;

    public static User CURRENT_USER;

    public static Gson GSON = new GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    //Use Genius to get artist songs
    public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException, ExecutionException, InterruptedException {
        URI REDIRECT_URL = SpotifyHttpManager.makeUri(PRIVATE_CONFIG.getConfigOptionNonNull("redirect-url"));

        API = new SpotifyApi.Builder()
                .setClientId(PRIVATE_CONFIG.getConfigOptionNonNull("client-id"))
                .setClientSecret(PRIVATE_CONFIG.getConfigOptionNonNull("client-secret"))
                .setRedirectUri(REDIRECT_URL)
                .build();

        if (!PRIVATE_CONFIG.hasConfigOption("auth-code")) {
            AuthorizationCodeUriRequest credentials = API
                    .authorizationCodeUri(PRIVATE_CONFIG.getConfigOptionNonNull("client-id"), REDIRECT_URL)
                    .scope(AuthorizationScope.values())
                    .build();

            URI uri = credentials.execute();

            LOGGER.info(uri.toString());

            LOGGER.info("Use code retrieved from redirect for config and then rerun");
            return;
        }

        if (!PRIVATE_CONFIG.hasConfigOption("access-token") || !PRIVATE_CONFIG.hasConfigOption("refresh-token")) {
            AuthorizationCodeCredentials request = API.authorizationCode(PRIVATE_CONFIG.getConfigOptionNonNull("auth-code")).build().execute();

            PRIVATE_CONFIG.setConfigOption("access-token", request.getAccessToken());
            PRIVATE_CONFIG.setConfigOption("refresh-token", request.getRefreshToken());
            PRIVATE_CONFIG.reload();
        }

        API.setRefreshToken(PRIVATE_CONFIG.getConfigOption("refresh-token"));
        API.setAccessToken(PRIVATE_CONFIG.getConfigOption("access-token"));

        try {
            CURRENT_USER = API.getCurrentUsersProfile().build().execute();
        } catch (UnauthorizedException e) {
            AuthorizationCodeCredentials request = API.authorizationCodeRefresh().refresh_token(API.getRefreshToken()).build().execute();

            PRIVATE_CONFIG.setConfigOption("access-token", request.getAccessToken());
            PRIVATE_CONFIG.reload();

            API.setAccessToken(PRIVATE_CONFIG.getConfigOption("access-token"));

            CURRENT_USER = API.getCurrentUsersProfile().build().execute();
        }

        if (!PRIVATE_CONFIG.hasConfigOption("genius-auth-code")) {
            GeniusAuthentication.buildAuthCodeLink();
            return;
        }

        if (!PRIVATE_CONFIG.hasConfigOption("genius-access-token")) {
            OAuth2AccessToken accessToken = GeniusAuthentication.retrieveAccessToken();

            PRIVATE_CONFIG.setConfigOption("genius-access-token", accessToken.getAccessToken());
            PRIVATE_CONFIG.setConfigOption("genius-access-token-type", accessToken.getTokenType());

            PRIVATE_CONFIG.reload();
        }


        LOGGER.info(CURRENT_USER.getId());

        Scanner scanner = new Scanner(System.in);

        LOGGER.info("Input 1 for Weekly Rotation, 2 for Liked Songs playlist, 3 to set player to random album, 4 to get Liked Songs Statistics, 5 to get top artists and tracks, 6 to put top tracks into a playlist, 7 to see similar playlists, 8 to get recomendations based on album, 9 to get recomendations based on song");

        int input = Integer.parseInt(scanner.nextLine());

        switch (input) {
            case 1 -> executeWeeklyRotation();
            case 2 -> {
                LOGGER.info("Input artist name");
                String name = scanner.nextLine();
                createPlaylistForArtist(name);
            }
            case 3 -> setPlayerToRandomAlbum();
            case 4 -> outputLikedSongsData();
            case 5 -> outputTopArtistsAndSongs();
            case 6 -> createTopTracksPlaylist();
            case 7 -> {
                LOGGER.info("Input Categories");
                LOGGER.info("Categories Are:");
                for (Category item : API.getListOfCategories().build().execute().getItems()) {
                    LOGGER.info(item.getName());
                }
                String categories = scanner.nextLine();
                outputPlaylistsSimilarToYourTastes(Stream
                        .of(categories.split("\\s+"))
                        .map(String::toLowerCase).toList());
            }
            case 8 -> {
                LOGGER.info("What album are you looking reccomendations on?");
                String line = scanner.nextLine();
                createPlaylistOfReccomendationsBasedOnAlbum(line);
            }
            case 9 -> {
                LOGGER.info("What single are you looking recommendations on?");
                String line = scanner.nextLine();
                createPlaylistOfReccomendationsBasedOnSingle(line);
            }
            case 10 -> sortAudioFeaturesAndOutput();
            default -> LOGGER.error("Incorrect Input!");
        }

        Response response = GeniusRequests.exampleRequest();

        LOGGER.info(GeniusRequests.beautifyResponse(response));

        response.close();
    }

    private static void sortAudioFeaturesAndOutput() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> savedTracks = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        Map<String, Number> map = new LinkedHashMap<>();
        for (List<SavedTrack> subListX : subListX(savedTracks, 100)) {
            for (AudioFeatures features : API.getAudioFeaturesForSeveralTracks(subListX
                .stream()
                .map(savedTrack -> savedTrack.getTrack().getId()).toArray(String[]::new)).build().execute()) {
                map.put(features.getId(), features.getTempo());
            }
        }

        map = sortArtistMap(map);

        LOGGER.info("Most energetic songs");
        for (Map.Entry<String, Number> entry : map.entrySet()) {
            String track = entry.getKey();
            Number number = entry.getValue();
            Track track1 = API.getTrack(track).build().execute();
            LOGGER.info(track1.getName() + " - " + toString(track1.getArtists(), ArtistSimplified::getName) + " (" + number.floatValue() + ")");
        }
    }

    private static Number average(Number... nums) {
        double total = 0;
        for (Number num : nums) {
            total += num.doubleValue();
        }

        return total/nums.length;
    }

    private static void createPlaylistOfReccomendationsBasedOnSingle(String line) throws IOException, ParseException, SpotifyWebApiException {
        Track track = API.searchTracks(line).build().execute().getItems()[0];

        List<String> artistIds = Arrays.stream(track.getArtists()).map(ArtistSimplified::getId).toList();
        List<TrackSimplified> recommendedTracks = List.of(API.getRecommendations().seed_artists(String.join(",", artistIds)).seed_genres("hip-hop").seed_tracks(track.getId()).limit(100).build().execute().getTracks());

        String name = "Recommendations for " + line;

        String playlistId = getAndDeleteSongsOrCreatePlaylist(name);

        List<String> finalTracks = new ArrayList<>();
        finalTracks.add("spotify:track:" + track.getId());


        finalTracks.addAll(recommendedTracks
            .stream()
            .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
            .toList());

        addTracksToPlaylist(playlistId, finalTracks);

        Image image = getBiggestImage(track.getAlbum().getImages());

        uploadImageToPlaylist(playlistId, image);
    }

    private static void uploadImageToPlaylist(String playlistId, Image image) throws IOException, SpotifyWebApiException, ParseException {
        URL imageUrl = new URL(image.getUrl());

        BufferedImage bufferedImage = ImageIO.read(imageUrl);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpeg", stream);
        byte[] data = stream.toByteArray();

        String imageString = Base64.encodeBase64String(data);

        API.uploadCustomPlaylistCoverImage(playlistId).image_data(imageString).build().execute();
    }

    private static void createPlaylistOfReccomendationsBasedOnAlbum(String albumName) throws IOException, ParseException, SpotifyWebApiException {
        AlbumSimplifiedSpecial album = API.searchAlbumsSpecial(albumName).build().execute().getItems()[0];

        List<String> artistIds = Arrays.stream(album.getArtists()).map(ArtistSimplified::getId).toList();
        int remainingSeeds = 4 - artistIds.size();
        GetRecommendationsRequest.Builder builder = API.getRecommendations().seed_artists(String.join(",", artistIds)).seed_genres("hip-hop");

        List<String> trackIds = new ArrayList<>();

        List<TrackSimplified> albumTracks = getTotalEntities(API.getAlbumsTracks(album.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getAlbumsTracks(album.getId()));
        Random random = new Random();

        for (int i = 0; i < remainingSeeds; i++) {
            TrackSimplified trackSimplified = albumTracks.get(random.nextInt(albumTracks.size()));
            trackIds.add(trackSimplified.getId());

            albumTracks.remove(trackSimplified);
        }

        builder.seed_tracks(String.join(",", trackIds));

        List<TrackSimplified> recommendedTracks = List.of(builder.limit(100).build().execute().getTracks());

        String name = "Recommendations for " + albumName;

        String playlistId = getAndDeleteSongsOrCreatePlaylist(name);

        List<String> finalTracks = new ArrayList<>(trackIds.stream().map(s -> "spotify:track:" + s).toList());

        finalTracks.addAll(recommendedTracks
                .stream()
                .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
                .toList());

        addTracksToPlaylist(playlistId, finalTracks);

        Image image = getBiggestImage(album.getImages());

        uploadImageToPlaylist(playlistId, image);
    }

    private static Image getBiggestImage(Image[] images) {
        int maxArea = images[0].getHeight() * images[1].getWidth();
        int index = 0;

        for (int i = 1; i < images.length; i++) {
            Image image = images[i];
            int area = image.getHeight() * image.getWidth();
            if (area > maxArea) {
                maxArea = area;
                index = i;
            }
        }

        return images[index];
    }

    private static void outputPlaylistsSimilarToYourTastes(List<String> desiredCategories) throws IOException, ParseException, SpotifyWebApiException {
        Map<PlaylistSimplified, Number> similarPlaylists = new LinkedHashMap<>();
        List<Track> tracks = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks).stream().map(SavedTrack::getTrack).toList();

        List<Category> categories = getTotalEntities(API.getListOfCategories().build().execute().getTotal(), SpotifyApi::getListOfCategories);

        LOGGER.info("Total # of Categories: " + categories.size());
        try {
            for (int i = 0; i < categories.size(); i++) {
                Category totalEntity = categories.get(i);
                if (!desiredCategories.contains(totalEntity.getName().toLowerCase())) {
                    continue;
                }
                LOGGER.info("Processing info for the category (" + (i + 1) + " out of " + categories.size() + ") : " + totalEntity.getName());
                List<PlaylistSimplified> playlists;
                try {
                    playlists = getTotalEntities(API.getCategorysPlaylists(totalEntity.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getCategorysPlaylists(totalEntity.getId()));
                } catch (Exception e) {
                    LOGGER.error("Error while getting playlists for category!", e);
                    continue;
                }
                LOGGER.info("# of Playlists in category: " + playlists.size());
                for (PlaylistSimplified entity : playlists) {
                    if (entity == null) {
                        continue;
                    }
                    LOGGER.info("Processing playlist: " + entity.getName());
                    LOGGER.info("https://open.spotify.com/playlist/" + entity.getId());
                    int tracksSimilar = 0;
                    for (PlaylistTrack item : API.getPlaylist(entity.getId()).build().execute().getTracks().getItems()) {
                        if (item.getTrack() instanceof Track track) {
                            if (tracks.contains(track)) {
                                tracksSimilar++;
                            }
                        }
                    }
                    similarPlaylists.put(entity, tracksSimilar);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Uncaught exception, stopping massive loop", e);
        }

        similarPlaylists = sortArtistMap(similarPlaylists);
        similarPlaylists.forEach((playlistSimplified, integer) -> {
            LOGGER.info(playlistSimplified.getName() + " - " + integer.intValue());
            LOGGER.info("https://open.spotify.com/playlist/" + playlistSimplified.getId());
        });
    }

    private static void createTopTracksPlaylist() throws IOException, ParseException, SpotifyWebApiException {
        String playlistId = getAndDeleteSongsOrCreatePlaylist(API.getCurrentUsersProfile().build().execute().getDisplayName() + "'s Top Songs");

        List<String> uris = getTotalEntities(API.getUsersTopTracks().build().execute().getTotal(), SpotifyApi::getUsersTopTracks)
            .stream()
            .map(track -> "spotify:track:" + track.getId())
            .toList();

        addTracksToPlaylist(playlistId, uris);
    }

    private static void addTracksToPlaylist(String playlistId, List<String> uris) throws IOException, SpotifyWebApiException, ParseException {
        List<List<String>> moreUris = subListX(uris, 50);

        for (List<String> strings : moreUris) {
            String[] uri = strings.toArray(String[]::new);
            API.addItemsToPlaylist(playlistId, uri).build().execute();
        }

        LOGGER.info("https://open.spotify.com/playlist/" + playlistId);
    }

    private static void outputTopArtistsAndSongs() throws IOException, ParseException, SpotifyWebApiException {
        LOGGER.info("Top Artists");
        getTotalEntities(API.getUsersTopArtists().build().execute().getTotal(), SpotifyApi::getUsersTopArtists).forEach(artist -> {
            LOGGER.info(artist.getName());
        });

        LOGGER.info("Top Tracks");
        getTotalEntities(API.getUsersTopTracks().build().execute().getTotal(), SpotifyApi::getUsersTopTracks).forEach(track -> {
            LOGGER.info(track.getName() + " - " + toString(track.getArtists(), ArtistSimplified::getName));
        });
    }

    private static void outputLikedSongsData() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> savedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        Map<ArtistSimplified, Number> likedSongsMap = new LinkedHashMap<>();

        for (SavedTrack savedTrack : savedSongs) {
            if (savedTrack == null) {
                continue;
            }

            ArtistSimplified[] artists = savedTrack.getTrack().getArtists();

            for (ArtistSimplified artist : artists) {
                if (likedSongsMap.containsKey(artist)) {
                    likedSongsMap.replace(artist, likedSongsMap.get(artist).intValue() + 1);
                } else {
                    likedSongsMap.put(artist, 1);
                }
            }
        }

        likedSongsMap = sortArtistMap(likedSongsMap);

        likedSongsMap.forEach((artistSimplified, integer) -> {
            LOGGER.info(artistSimplified.getName() + " - " + integer.intValue() + " Liked Songs");
        });
    }

    public static <T> Map<T, Number> sortArtistMap(Map<T, Number> map) {
        List<Map.Entry<T, Number>> list = new LinkedList<>(map.entrySet());

        list.sort(Map.Entry.comparingByValue((o1, o2) -> -compare(o1, o2)));

        Map<T, Number> map2 = new LinkedHashMap<>();

        for (Map.Entry<T, Number> artistSimplifiedIntegerEntry : list) {
            map2.put(artistSimplifiedIntegerEntry.getKey(), artistSimplifiedIntegerEntry.getValue());
        }

        return map2;
    }

    private static int compare(Number x, Number y) {
        return Double.compare(x.doubleValue(), y.doubleValue());
    }



    private static void setPlayerToRandomAlbum() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedAlbum> savedAlbums = getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums);

        Random random = new Random();

        SavedAlbum randomAlbum = savedAlbums.get(random.nextInt(savedAlbums.size()));


        List<String> tracks = Arrays.stream(randomAlbum.getAlbum().getTracks().getItems())
            .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
            .toList();

        for (String s : tracks) {
            API.addItemToUsersPlaybackQueue(s).build().execute();
        }

        LOGGER.info(randomAlbum.getAlbum().getName() + " - " + toString(randomAlbum.getAlbum().getArtists(), ArtistSimplified::getName) + " added to queue");


    }

    public static void createPlaylistForArtist(String artist) throws IOException, ParseException, SpotifyWebApiException {
        String playlistId = getAndDeleteSongsOrCreatePlaylist(artist + " Bangers");

        List<SavedTrack> tracks = getTracksWithArtist(artist);

        List<String> uris = tracks
            .stream()
            .map(savedTrack -> "spotify:track:" + savedTrack.getTrack().getId())
            .toList();

        addTracksToPlaylist(playlistId, uris);
    }

    private static String getAndDeleteSongsOrCreatePlaylist(String name) throws IOException, ParseException, SpotifyWebApiException {
        String playlistId = null;

        for (PlaylistSimplified playlist : getTotalEntities(API.getListOfCurrentUsersPlaylists().build().execute().getTotal(), SpotifyApi::getListOfCurrentUsersPlaylists)) {
            if (playlist.getName().contains(name)) {
                playlistId = playlist.getId();
            }
        }

        if (playlistId != null) {
            LOGGER.info(name + " playlist already present, deleting songs in old playlist...");

            deleteAllSongsInPlaylist(playlistId);
        } else {
            LOGGER.info("Playlist not present... Creating now");
            playlistId = API.createPlaylist(CURRENT_USER.getId(), name)
                .public_(false)
                .build()
                .execute()
                .getId();

            API.changePlaylistsDetails(playlistId).public_(false).build().execute();
        }

        return playlistId;
    }

    private static List<SavedTrack> getTracksWithArtist(String artist) throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> likedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        List<SavedTrack> artistSongs = new ArrayList<>();

        likedSongs.forEach(savedTrack -> {
            String artists = toString(savedTrack.getTrack().getArtists(), ArtistSimplified::getName);
            if (artists.contains(artist)) {
                artistSongs.add(savedTrack);
            }
        });

        return artistSongs;
    }

    private static <T> String toString(T[] array, Function<T, String> function) {
        StringBuilder builder = new StringBuilder("By ");
        for (int i = 0; i < array.length; i++) {
            T t = array[i];
            builder.append(function.apply(t));

            if (i + 1 != array.length) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public static void executeWeeklyRotation() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> savedTracks = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        List<SavedTrack> recentTracks = getTracksWithinLastXMonths(savedTracks, GENERAL_CONFIG.getConfigOption("recent-album-month-limit", Integer::parseInt));

        List<SavedTrack> weeklyRotation = createWeeklyRotation(savedTracks, recentTracks);

        String weeklyRotationName = getWeeklyRotationPlaylistName();

        LOGGER.info(weeklyRotationName + " Weekly Rotation");

        weeklyRotation.forEach(savedTrack -> LOGGER.info(savedTrack.getTrack().getName() + " - " + savedTrack.getTrack().getAlbum().getReleaseDate()));

        String playlistId = getAndDeleteSongsOrCreatePlaylist(weeklyRotationName + " Weekly Rotation");

        String[] uris = weeklyRotation
            .stream()
            .map(savedTrack -> "spotify:track:" + savedTrack.getTrack().getId())
            .toArray(String[]::new);

        API.addItemsToPlaylist(playlistId, uris).build().execute();

        LOGGER.info("https://open.spotify.com/playlist/" + playlistId);
    }

    private static void deleteAllSongsInPlaylist(String playlistId) throws IOException, ParseException, SpotifyWebApiException {
        JsonArray array = new JsonArray();

        for (PlaylistTrack item : API.getPlaylist(playlistId).build().execute().getTracks().getItems()) {
            String id = item.getTrack().getId();

            if (item.getIsLocal()) {
                continue;
            }

            JsonObject object = new JsonObject();
            object.addProperty("uri", "spotify:track:" + id);
            array.add(object);
        }


        API.removeItemsFromPlaylist(playlistId, array).build().execute();
    }

    public static String getWeeklyRotationPlaylistName() {
        LocalDate now = LocalDate.now();

        int daysToSubtract = Math.abs(1 - now.getDayOfWeek().getValue());

        LocalDate monday = now.minusDays(daysToSubtract);

        LocalDate sunday = monday.plusDays(6);

        return localDateToMonthDay(monday) + " - " + localDateToMonthDay(sunday);
    }

    public static String localDateToMonthDay(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public static List<SavedTrack> createWeeklyRotation(List<SavedTrack> savedTracks, List<SavedTrack> recentTracks) {
        List<SavedTrack> weeklyRotation = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < GENERAL_CONFIG.getConfigOption("recent-songs-limit", Integer::parseInt); i++) {
            weeklyRotation.add(recentTracks.get(random.nextInt(recentTracks.size())));
        }

        int currentSize = weeklyRotation.size();

        for (int i = 0; i < (GENERAL_CONFIG.getConfigOption("weekly-rotation-song-limit", Integer::parseInt) - currentSize); i++) {
            weeklyRotation.add(savedTracks.get(random.nextInt(savedTracks.size())));
        }

        return weeklyRotation;
    }

    public static List<SavedTrack> getTracksWithinLastXMonths(List<SavedTrack> savedTracks, int months) {
        List<SavedTrack> savedTracks1 = new ArrayList<>();

        savedTracks.forEach(savedTrack -> {
           Track track = savedTrack.getTrack();

           if (track.getAlbum().getReleaseDatePrecision() != ReleaseDatePrecision.DAY) {
               return;
           }

           LocalDate date = releaseDateToLocalDate(track.getAlbum().getReleaseDate());

           if (date.isAfter(LocalDate.now().minusMonths(months))) {
               savedTracks1.add(savedTrack);
           }
        });

        return savedTracks1;
    }

    public static LocalDate releaseDateToLocalDate(String releaseDate) {
        Integer[] releaseDateSplit = Arrays.stream(releaseDate.split("-")).map(Integer::parseInt).toArray(Integer[]::new);
        return LocalDate.of(releaseDateSplit[0], releaseDateSplit[1], releaseDateSplit[2]);
    }

    public static <T> List<T> getTotalEntities(int total, Function<SpotifyApi,? extends AbstractDataPagingRequest.Builder<T, ?>> function) throws IOException, ParseException, SpotifyWebApiException {
        List<T> entities = new ArrayList<>();

        for (int i = 0; i < total; i += 50) {
            entities.addAll(Arrays.asList(function
                .apply(API)
                .limit(50)
                .offset(i)
                .build()
                .execute()
                .getItems()));
        }

        return entities;
    }

    // creates lists from a master list with size x
    public static <T> List<List<T>> subListX(List<T> list, int x) {
        List<List<T>> returnList = new ArrayList<>();

        for (int i = 0; i < list.size(); i += x) {
            returnList.add(list.subList(i, Math.min(i + x, list.size())));
        }

        return returnList;
    }

}
