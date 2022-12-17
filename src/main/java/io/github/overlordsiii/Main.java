package io.github.overlordsiii;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overlordsiii.config.PropertiesHandler;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.enums.ReleaseDatePrecision;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.library.GetUsersSavedTracksRequest;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {

    public static Logger LOGGER = LoggerFactory.getLogger("Weekly Rotation");

    private static URI REDIRECT_URL;

    public static PropertiesHandler PRIVATE_CONFIG = PropertiesHandler
        .builder()
        .addConfigOption("client-id", "")
        .addConfigOption("client-secret", "")
        .addConfigOption("auth-code", "")
        .addConfigOption("access-token", "")
        .addConfigOption("refresh-token", "")
        .addConfigOption("redirect-url", "https://example.com/spotify-redirect")
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

    public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException {
        REDIRECT_URL = SpotifyHttpManager.makeUri(PRIVATE_CONFIG.getConfigOptionNonNull("redirect-url"));

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


        CURRENT_USER = API.getCurrentUsersProfile().build().execute();

        LOGGER.info(CURRENT_USER.getId());

        List<SavedTrack> savedTracks = getSavedTracks();

        List<SavedTrack> recentTracks = getTracksWithinLastXMonths(savedTracks, GENERAL_CONFIG.getConfigOption("recent-album-month-limit", Integer::parseInt));

        List<SavedTrack> weeklyRotation = createWeeklyRotation(savedTracks, recentTracks);

        String weeklyRotationName = getWeeklyRotationPlaylistName();

        LOGGER.info(weeklyRotationName + " Weekly Rotation");

        weeklyRotation.forEach(savedTrack -> LOGGER.info(savedTrack.getTrack().getName() + " - " + savedTrack.getTrack().getAlbum().getReleaseDate()));

        String playlistId = null;

        List<PlaylistSimplified> playlists = getUserPlaylists();


        for (PlaylistSimplified playlist : playlists) {
            if (playlist.getName().startsWith(getWeeklyRotationPlaylistName())) {
                playlistId = playlist.getId();
            }
        }

        if (playlistId != null) {
            LOGGER.info("Playlist already created... Deleting previous tracks");
            JsonArray array = new JsonArray();

            for (PlaylistTrack item : API.getPlaylist(playlistId).build().execute().getTracks().getItems()) {
                String id = item.getTrack().getId();

                JsonObject object = new JsonObject();
                object.addProperty("uri", "spotify:track:" + id);
                array.add(object);
            }


            API.removeItemsFromPlaylist(playlistId, array).build().execute();
        } else {
            LOGGER.info("Playlist not present... Creating now");
            playlistId = API.createPlaylist(CURRENT_USER.getId(), weeklyRotationName + " Weekly Rotation")
                .description(getPlaylistDescription())
                .public_(GENERAL_CONFIG.getConfigOption("weekly-rotation-playlist-public", Boolean::parseBoolean))
                .build()
                .execute()
                .getId();
        }

        String[] uris = weeklyRotation
            .stream()
            .map(savedTrack -> "spotify:track:" + savedTrack.getTrack().getId())
            .toArray(String[]::new);

        API.addItemsToPlaylist(playlistId, uris).build().execute();

        LOGGER.info("https://open.spotify.com/playlist/" + playlistId);
    }

    public static String getPlaylistDescription() {
        return "Weekly Rotation playlist for the week of " + getWeeklyRotationPlaylistName() + ". It consists of " + GENERAL_CONFIG.getConfigOption("weekly-rotation-song-limit") + " songs including " + GENERAL_CONFIG.getConfigOption("recent-album-month-limit") + " recent songs. Based on the liked songs of " + CURRENT_USER.getDisplayName() + ". Created by Weekly Rotation bot (https://github.com/OverlordsIII/WeeklyRotation)";
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



    public static List<SavedTrack> getSavedTracks() throws IOException, ParseException, SpotifyWebApiException {
        int totalLikedSongs = API.getUsersSavedTracks().build().execute().getTotal();

        List<SavedTrack> savedTracks = new ArrayList<>();

        for (int i = 0; i < totalLikedSongs; i += 50) {
            savedTracks.addAll(Arrays.asList(API
                .getUsersSavedTracks()
                .limit(50)
                .offset(i)
                .build()
                .execute()
                .getItems()));
        }

        return savedTracks;
    }

    public static List<PlaylistSimplified> getUserPlaylists() throws IOException, ParseException, SpotifyWebApiException {
        int totalPlaylists = API.getListOfCurrentUsersPlaylists().build().execute().getTotal();

        List<PlaylistSimplified> playlists = new ArrayList<>();

        for (int i = 0; i < totalPlaylists; i += 50) {
            playlists.addAll(Arrays.asList(API
                .getListOfCurrentUsersPlaylists()
                .limit(50)
                .offset(i)
                .build()
                .execute()
                .getItems()
            ));
        }

        return playlists;
    }

}
