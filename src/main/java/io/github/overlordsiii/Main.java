package io.github.overlordsiii;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.overlordsiii.config.PropertiesHandler;
import io.github.overlordsiii.genius.GeniusRequests;
import io.github.overlordsiii.ranking.RankedObject;
import io.github.overlordsiii.ranking.RankingProgram;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AlbumType;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.enums.ReleaseDatePrecision;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.AbstractModelObject;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.special.AlbumSimplifiedSpecial;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.AbstractDataPagingRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            .addConfigOption("genius-backend-path", "https://genius.com/api/")
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

    public static Random RANDOM = new Random();

    public static Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    // Use genius to get duo collabs
    public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
        refreshAPI();


        LOGGER.info(CURRENT_USER.getId());

        Scanner scanner = new Scanner(System.in);

        LOGGER.info("Input 1 for Weekly Rotation, 2 for Liked Songs playlist, 3 to set player to random album, 4 to get Liked Songs Statistics, 5 to get top artists and tracks, 6 to put top tracks into a playlist, 7 to see similar playlists, 8 to get recomendations based on album, 9 to get recomendations based on song");

        int input = Integer.parseInt(scanner.nextLine());

        switch (input) {
            case 1 -> executeWeeklyRotation();
            case 2 -> {
                LOGGER.info("Input artist name");
                String name = scanner.nextLine();
                LOGGER.info("Bypass genius?");
                boolean bypass = scanner.nextBoolean();
                createPlaylistForArtist(false, bypass, name);
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
                LOGGER.info("What genre is it in?");
                String genre = scanner.nextLine().trim();
                createPlaylistOfReccomendationsBasedOnAlbum(line, genre);
            }
            case 9 -> {
                LOGGER.info("What single are you looking recommendations on?");
                String line = scanner.nextLine();
                LOGGER.info("What genre is it in?");
                String genre = scanner.nextLine().trim();
                createPlaylistOfReccomendationsBasedOnSingle(line, genre);
            }
            case 10 -> sortAudioFeaturesAndOutput();
            case 11 -> {
                LOGGER.info("Input artist 1");
                String artist1 = scanner.nextLine();
                LOGGER.info("Input artist 2");
                String artist2 = scanner.nextLine();
                LOGGER.info("Bypass Genius?");
                boolean bypass = scanner.nextBoolean();
                createPlaylistForArtist(true, bypass, artist1, artist2);
            }
            // create playlist with artist + producer
            case 12 -> {
                LOGGER.info("Input artist");
                String artist = scanner.nextLine();
                LOGGER.info("Input producer");
                String producer = scanner.nextLine();
                createPlaylistForArtistAndProducer(artist, producer);
            }
            case 13 -> {
                LOGGER.info("Only unliked albums or no?");
                boolean bool = scanner.nextBoolean();
                outputLikedSongsAlbumData(bool);
            }
            case 14 -> refreshPlaylists();
            case 15 -> outputSongsFromLikedAlbums();
            case 16 -> outputSavedSongsWithoutVowels();
            case 17 -> outputPopularitySongs();
            case 18 -> {
                LOGGER.info("Input an artist to rank");
                String artist = scanner.nextLine();
                rankArtistSongs(artist);
            }
            case 19 -> {
                LOGGER.info("Input an album to rank");
                String album = scanner.nextLine();
                rankAlbumSongs(album);
            }
            case 20 -> rankUserAlbums();
            case 21 -> createFavSongsByArtistsPlaylist();
            case 22 -> outputRandomAlbums();
            case 23 -> {
                LOGGER.info("Input Genre");
                String genre = scanner.nextLine();
                getAlbumsByGenre(genre);
            }

            default -> LOGGER.error("Incorrect Input!");
        }
    }

    private static void getAlbumsByGenre(String genre) throws IOException, ParseException, SpotifyWebApiException {
        List<SavedAlbum> savedAlbums = getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums);

        LOGGER.info("# of Albums: " + savedAlbums.size());

        List<String> console = new ArrayList<>();

        for (SavedAlbum savedAlbum : savedAlbums) {
            Album album = savedAlbum.getAlbum();
            StringBuilder builder = new StringBuilder();

            for (ArtistSimplified artist : album.getArtists()) {
                LOGGER.info("Artist: " + artist.getName());
                builder.append(Arrays.toString(API.getArtist(artist.getId()).build().execute().getGenres()));
            }

            String genres = builder.toString();

            if (genres.toLowerCase().contains(genre.toLowerCase())) {

                console.add(album.getName() + " - " + toString(album.getArtists(), ArtistSimplified::getName));
            }
        }

        console.forEach(LOGGER::info);

    }

    private static void outputRandomAlbums() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedAlbum> savedAlbums = getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums);

        LOGGER.info("# of Albums: " + savedAlbums.size());

        for (int i = 0; i < 5; i++) {
            Album album = savedAlbums.get(RANDOM.nextInt(savedAlbums.size())).getAlbum();
            LOGGER.info(album.getName() + " - " + toString(album.getArtists(), ArtistSimplified::getName));
        }



    }

    private static void createFavSongsByArtistsPlaylist() throws IOException, ParseException, SpotifyWebApiException {
        List<Artist> artists = getTotalEntities(API.getUsersTopArtists().build().execute().getTotal(), SpotifyApi::getUsersTopArtists);

        String playlistId = getAndDeleteSongsOrCreatePlaylist("LeArtistADay");

        List<Track> tracks = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        for (Artist artist : artists) {
            LOGGER.info("Artist: " + artist.getName());
            LOGGER.info("Choose a song by artist: ");
            String songName = scanner.nextLine();

            tracks.add(API.searchTracks(songName + " " + artist.getName()).build().execute().getItems()[0]);
        }

        addTracksToPlaylist(playlistId, tracks
            .stream()
            .map(track -> "spotify:track:" + track.getId())
            .toList());
    }

    private static void rankUserAlbums() throws IOException, ParseException, SpotifyWebApiException {
        List<RankedObject<Album>> albums = new ArrayList<>(getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums)
                .stream()
                .map(savedAlbum -> new RankedObject<>(savedAlbum.getAlbum(), savedAlbum.getAlbum().getName()))
                .toList());

        RankingProgram.rank(albums);


    }

    private static void rankAlbumSongs(String album) throws IOException, ParseException, SpotifyWebApiException {
        String albumId = API.searchAlbumsSpecial(album).build().execute().getItems()[0].getId();

        List<RankedObject<TrackSimplified>> tracks = new ArrayList<>();
        for (TrackSimplified item : API.getAlbumsTracks(albumId).build().execute().getItems()) {
            LOGGER.info("Processing Track: " + item.getName());
            tracks.add(new RankedObject<>(item, item.getName()));
        }

        RankingProgram.rank(tracks);
    }

    private static void rankArtistSongs(String artistStr) throws IOException, ParseException, SpotifyWebApiException {
        String artistId = API.searchArtists(artistStr).build().execute().getItems()[0].getId();

        List<RankedObject<TrackSimplified>> tracks = new ArrayList<>();
        List<AlbumSimplified> list = getAllPagingItems(API.getArtistsAlbums(artistId))
            .stream()
            .distinct()
            .filter(albumSimplified -> albumSimplified.getArtists()[0].getId().equals(artistId))
            .toList();
        for (AlbumSimplified allPagingItem : list) {
            LOGGER.info("Processing Album: " + allPagingItem.getName());
            for (TrackSimplified item : API.getAlbumsTracks(allPagingItem.getId()).build().execute().getItems()) {
                LOGGER.info("Processing Track: " + item.getName());
                tracks.add(new RankedObject<>(item, item.getName()));
            }
        }

        RankingProgram.rank(tracks);
    }

    private static void outputPopularitySongs() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> likedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        Map<Track, Number> map = new LinkedHashMap<>();

        for (SavedTrack savedTrack : likedSongs) {
            map.put(savedTrack.getTrack(), savedTrack.getTrack().getPopularity());
        }

        map = sortArtistMap(map);

        map.forEach((track, number) -> {
            LOGGER.info(track.getName() + " " + toString(track.getArtists(), ArtistSimplified::getName) + " (" + number + ")");
        });

    }

    private static void outputSavedSongsWithoutVowels() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> likedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks).stream().distinct().toList();

        List<SavedTrack> songs = new ArrayList<>();

        likedSongs.forEach(savedTrack -> {
            boolean add = true;
            for (char c : savedTrack.getTrack().getName().toCharArray()) {
                c = Character.toLowerCase(c);
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                    add = false;
                    break;
                }
            }

            if (add) {
                songs.add(savedTrack);
            }
        });

        songs.forEach(savedTrack -> {
            LOGGER.info(savedTrack.getTrack().getName() + " " + toString(savedTrack.getTrack().getArtists(), ArtistSimplified::getName));
        });
    }

    private static void outputSongsFromLikedAlbums() throws IOException, ParseException, SpotifyWebApiException {
        List<SavedAlbum> savedAlbums = getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums);

        List<String> songIdMap = savedAlbums
            .stream()
            .flatMap(savedAlbum -> {
                LOGGER.info("Processing: " + savedAlbum.getAlbum().getName() + " " + toString(savedAlbum.getAlbum().getArtists(), ArtistSimplified::getName));
                try {
                    return Arrays.stream(API.getAlbumsTracks(savedAlbum.getAlbum().getId()).build().execute().getItems()).map(TrackSimplified::getId);
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

        List<String> likedSongIdMap = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks).stream().map(savedTrack -> savedTrack.getTrack().getId()).toList();

        int partOfSavedAlbums = 0;

        for (String s : likedSongIdMap) {
            if (songIdMap.contains(s)) {
                partOfSavedAlbums++;
            }
        }

        int notPartOfSavedAlbum = likedSongIdMap.size() - partOfSavedAlbums;

        LOGGER.info("Liked Songs that are part of Saved Albums: " + partOfSavedAlbums + " (" +  100*((partOfSavedAlbums*1.0) / likedSongIdMap.size()) + "%)");
        LOGGER.info("Liked Songs that are not part of Saved Albums: " + notPartOfSavedAlbum + " (" +  100*((notPartOfSavedAlbum*1.0) / likedSongIdMap.size()) + "%)");


    }

    private static void refreshPlaylists() throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
        List<PlaylistSimplified> playlists = getTotalEntities(API.getListOfCurrentUsersPlaylists().build().execute().getTotal(), SpotifyApi::getListOfCurrentUsersPlaylists);

        Map<Artist, List<Track>> map = new HashMap<>();

        for (PlaylistSimplified playlist : playlists) {
            String desc = API.getPlaylist(playlist.getId()).build().execute().getDescription();

            if (desc.startsWith("duo-playlist")) {
                createPlaylistForArtist(true, false, getArtistsFromDesc(desc).toArray(String[]::new));
            } else if (desc.startsWith("producer-playlist")) {
                List<String> producerArt = getArtistsFromDesc(desc);
                createPlaylistForArtistAndProducer(producerArt.get(0), producerArt.get(1));
            } else if (desc.startsWith("top-tracks-playlist")) {
                createTopTracksPlaylist();
            } else if (desc.startsWith("single-playlist")) {
                map.put(API.searchArtists(getArtistsFromDesc(desc).get(0)).build().execute().getItems()[0], new ArrayList<>());
            }
        }

        for (SavedTrack totalEntity : getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks)) {

            String geniusStr = GeniusRequests.getArtistsOnSong(totalEntity.getTrack().getName(), totalEntity.getTrack().getArtists()[0].getName());
            String spotifyStr = toString(totalEntity.getTrack().getArtists(), ArtistSimplified::getName);


            for (Artist artist : map.keySet()) {
                Track track = null;
                if (geniusStr != null && geniusStr.contains(artist.getName())) {
                    track = totalEntity.getTrack();
                }

                if (spotifyStr.contains(artist.getName()) && track == null) {
                    track = totalEntity.getTrack();
                }

                if (track != null) {
                    map.get(artist).add(track);
                }
            }
        }

        for (Map.Entry<Artist, List<Track>> entry : map.entrySet()) {
            Artist artist = entry.getKey();
            List<Track> tracks = entry.getValue();
            String playlistId = getAndDeleteSongsOrCreatePlaylist(artist.getName() + " Bangers");

            addTracksToPlaylist(playlistId, tracks
                    .stream()
                    .map(track -> "spotify:track:" + track.getId())
                    .toList());

            uploadImageToPlaylist(playlistId, getBiggestImage(artist.getImages()));
        }


    }

    private static List<String> getArtistsFromDesc(String desc) {
        String array = desc.substring(desc.indexOf("[") + 1, desc.indexOf("]"));
        return Arrays.asList(array.split(":"));
    }

    public static void refreshAPI() throws IOException, ParseException, SpotifyWebApiException {
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
    }

    private static void outputLikedSongsAlbumData(boolean unlikedAlbumsOnly) throws IOException, ParseException, SpotifyWebApiException {
        List<SavedTrack> likedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks);

        Map<AlbumSimplified, Number> albumSimplifiedNumberMap = new LinkedHashMap<>();

        List<String> savedAlbumIds = getTotalEntities(API.getCurrentUsersSavedAlbums().build().execute().getTotal(), SpotifyApi::getCurrentUsersSavedAlbums)
                .stream()
                .map(SavedAlbum::getAlbum)
                .map(Album::getId)
                .toList();

        likedSongs.forEach(savedTrack -> {
            AlbumSimplified simplified = savedTrack.getTrack().getAlbum();

            if (simplified.getAlbumType() != AlbumType.ALBUM) {
                return;
            }

            if (unlikedAlbumsOnly && savedAlbumIds.contains(simplified.getId())) {
                return;
            }

            if (!albumSimplifiedNumberMap.containsKey(simplified)) {
                albumSimplifiedNumberMap.put(simplified, 1);
            } else {
                albumSimplifiedNumberMap.replace(simplified, albumSimplifiedNumberMap.get(simplified).intValue() + 1);
            }
        });

        Map<AlbumSimplified, Number> albumSimplifiedFloatMap = new LinkedHashMap<>();

        for (Map.Entry<AlbumSimplified, Number> entry : albumSimplifiedNumberMap.entrySet()) {
            AlbumSimplified albumSimplified = entry.getKey();
            Number number = entry.getValue();
            albumSimplifiedFloatMap.put(albumSimplified, number.floatValue() / API.getAlbumsTracks(albumSimplified.getId()).build().execute().getTotal());
        }

        albumSimplifiedFloatMap = sortArtistMap(albumSimplifiedFloatMap);

        albumSimplifiedFloatMap.forEach((albumSimplified, number) -> {
            BigDecimal bigDecimal = BigDecimal.valueOf(100 * number.doubleValue()).round(new MathContext(3));
            LOGGER.info(albumSimplified.getName() + " " + toString(albumSimplified.getArtists(), ArtistSimplified::getName) + " - " + bigDecimal + "%");
        });
    }

    private static void createPlaylistForArtistAndProducer(String artist, String producer) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {

        String playlistId = getAndDeleteSongsOrCreatePlaylist(artist + " with Production by " + producer);

        Artist artist1 = API.searchArtists(artist).build().execute().getItems()[0];
        List<TrackSimplified> tracks = new ArrayList<>();
        List<String> albumNames = new ArrayList<>();
        for (AlbumSimplified albumSimplified : getAllPagingItems(API.getArtistsAlbums(artist1.getId()))) {

            if (!albumNames.contains(albumSimplified.getName()) && toString(albumSimplified.getArtists(), ArtistSimplified::getName).contains(artist)) {
                albumNames.add(albumSimplified.getName());
                for (TrackSimplified track : getTotalEntities(API.getAlbumsTracks(albumSimplified.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getAlbumsTracks(albumSimplified.getId()))) {
                    if (GeniusRequests.isProducerOnSong(track.getName(), track.getArtists()[0].getName(), producer)) {
                        tracks.add(track);
                    }
                }
            }
        }

        Artist producerArtist = API.searchArtists(producer).build().execute().getItems()[0];

        if (!producerArtist.getName().contains(producer)) {
            return;
        }

        for (AlbumSimplified albumSimplified : getAllPagingItems(API.getArtistsAlbums(producerArtist.getId()))) {
            if (!albumNames.contains(albumSimplified.getName())) {
                albumNames.add(albumSimplified.getName());

                getTotalEntities(API.getAlbumsTracks(albumSimplified.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getAlbumsTracks(albumSimplified.getId())).forEach(trackSimplified -> {
                    if (toString(trackSimplified.getArtists(), ArtistSimplified::getName).contains(artist)) {
                        tracks.add(trackSimplified);
                    }
                });
            }
        }

        List<String> uris = tracks
                .stream()
                .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
                .toList();

        addTracksToPlaylist(playlistId, uris);

        Image image = getBiggestImage(API.getTrack(tracks.get(RANDOM.nextInt(tracks.size())).getId()).build().execute().getAlbum().getImages());

        uploadImageToPlaylist(playlistId, image);

        API.changePlaylistsDetails(playlistId).description("producer-playlist: [" + artist + ":" + producer + "]").build().execute();
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

        return total / nums.length;
    }

    private static void createPlaylistOfReccomendationsBasedOnSingle(String line, String genre) throws IOException, ParseException, SpotifyWebApiException {
        Track track = API.searchTracks(line).build().execute().getItems()[0];

        List<String> artistIds = Arrays.stream(track.getArtists()).map(ArtistSimplified::getId).toList();
        List<TrackSimplified> recommendedTracks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            recommendedTracks.addAll(List.of(API.getRecommendations().seed_artists(String.join(",", artistIds)).seed_genres(genre).seed_tracks(track.getId()).limit(100).build().execute().getTracks()));
        }

        recommendedTracks = getTopXSortedTracks(recommendedTracks, 100);


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

    private static <T> List<T> getTopXSortedTracks(List<T> totalTracks, int x) {
        List<T> list = new ArrayList<>();

        Map<T, Number> map = new LinkedHashMap<>();

        for (T trackSimplified : totalTracks) {
            if (!map.containsKey(trackSimplified)) {
                map.put(trackSimplified, 1);
            } else {
                map.replace(trackSimplified, map.get(trackSimplified).intValue() + 1);
            }
        }

        map = sortArtistMap(map);

        List<Map.Entry<T, Number>> entries = new ArrayList<>(map.entrySet());
        int index = x;
        if (entries.size() < x) {
            index = entries.size();
        }

        for (int i = 0; i < index; i++) {
            //  LOGGER.info(entries.get(i).getKey() + " - " + entries.get(i).getValue().intValue());
            list.add(entries.get(i).getKey());
        }

        return list;
    }

    private static void uploadImageToPlaylist(String playlistId, Image image) throws IOException, SpotifyWebApiException, ParseException {
        URL imageUrl = new URL(image.getUrl());

        BufferedImage bufferedImage = ImageIO.read(imageUrl);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpeg", stream);
        byte[] data = stream.toByteArray();

        String imageString = Base64.encodeBase64String(data);

        try {
            API.uploadCustomPlaylistCoverImage(playlistId).image_data(imageString).build().execute();
        } catch (Exception e) {
            LOGGER.error("Error while uploading image for playlist with id: " + playlistId, e);
        }
    }

    private static void createPlaylistOfReccomendationsBasedOnAlbum(String albumName, String genreName) throws IOException, ParseException, SpotifyWebApiException {
        AlbumSimplifiedSpecial album = API.searchAlbumsSpecial(albumName).build().execute().getItems()[0];

        List<String> artistIds = Arrays.stream(album.getArtists()).map(ArtistSimplified::getId).toList();
        int remainingSeeds = 4 - artistIds.size();
        List<TrackSimplified> albumTracks = getTotalEntities(API.getAlbumsTracks(album.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getAlbumsTracks(album.getId()));
        List<List<TrackSimplified>> simplifiedTracks = subListX(albumTracks, remainingSeeds);

        List<TrackSimplified> recommendedTracks = new ArrayList<>();

        for (List<TrackSimplified> simplifiedTrack : simplifiedTracks) {
            recommendedTracks.addAll(List.of(API.getRecommendations()
                    .seed_artists(String.join(",", artistIds))
                    .seed_genres(genreName)
                    .seed_tracks(simplifiedTrack
                            .stream()
                            .map(TrackSimplified::getId)
                            .collect(Collectors.joining(",")))
                    .limit(100)
                    .build()
                    .execute().getTracks()));

            simplifiedTrack.forEach(trackSimplified -> {
                LOGGER.info(trackSimplified.getName() + " - " + toString(trackSimplified.getArtists(), ArtistSimplified::getName));
            });
        }

        recommendedTracks = getTopXSortedTracks(recommendedTracks, 100);

        String name = "Recommendations for " + albumName;

        String playlistId = getAndDeleteSongsOrCreatePlaylist(name);

        List<String> finalTracks = recommendedTracks
                .stream()
                .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
                .toList();

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

        List<Track> tracks = getTotalEntities(API.getUsersTopTracks().build().execute().getTotal(), SpotifyApi::getUsersTopTracks);

        List<String> uris = tracks
                .stream()
                .map(track -> "spotify:track:" + track.getId())
                .toList();

        addTracksToPlaylist(playlistId, uris);

        Map<AlbumSimplified, Number> map = getAlbumStatistics(tracks);

        Image image = getBiggestImage(map.entrySet().stream().toList().get(0).getKey().getImages());

        uploadImageToPlaylist(playlistId, image);

        API.changePlaylistsDetails(playlistId).description("top-tracks-playlist").build().execute();

        Map<ArtistSimplified, Number> artistSimplifiedNumberMap = new LinkedHashMap<>();

        for (Track track : tracks) {
            if (track == null) {
                continue;
            }

            ArtistSimplified[] artists = track.getArtists();

            for (ArtistSimplified artist : artists) {
                if (artistSimplifiedNumberMap.containsKey(artist)) {
                    artistSimplifiedNumberMap.replace(artist, artistSimplifiedNumberMap.get(artist).intValue() + 1);
                } else {
                    artistSimplifiedNumberMap.put(artist, 1);
                }
            }
        }

        artistSimplifiedNumberMap = sortArtistMap(artistSimplifiedNumberMap);

        artistSimplifiedNumberMap.forEach((artistSimplified, integer) -> {
            LOGGER.info(artistSimplified.getName() + " - " + 100 * (integer.intValue() / 50.0) + "%");
        });
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
            BigDecimal bigDecimal = BigDecimal.valueOf(100 * (integer.intValue() / (double) savedSongs.size())).round(new MathContext(3));
            LOGGER.info(artistSimplified.getName() + " - " + integer.intValue() + " (" + bigDecimal + "%) Liked Songs");
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

        LOGGER.info("# Of Albums: " + savedAlbums.size());

        SavedAlbum randomAlbum = savedAlbums.get(RANDOM.nextInt(savedAlbums.size()));


        List<String> tracks = Arrays.stream(randomAlbum.getAlbum().getTracks().getItems())
                .map(trackSimplified -> "spotify:track:" + trackSimplified.getId())
                .toList();

        for (String s : tracks) {
            API.addItemToUsersPlaybackQueue(s).build().execute();
        }

        LOGGER.info(randomAlbum.getAlbum().getName() + " - " + toString(randomAlbum.getAlbum().getArtists(), ArtistSimplified::getName) + " added to queue");

        LOGGER.info("Tracks: ");
        for (TrackSimplified item : randomAlbum.getAlbum().getTracks().getItems()) {
            LOGGER.info(item.getName() + " - " + toString(item.getArtists(), ArtistSimplified::getName));
        }


    }

    public static void createPlaylistForArtist(boolean checkAll, boolean bypass, String... artist) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
        String playlistId = getAndDeleteSongsOrCreatePlaylist(String.join(" and ", artist) + " Bangers");

        List<String> tracks = getTracksWithArtist(checkAll, bypass, artist);

        List<String> uris = tracks
                .stream()
                .map(savedTrack -> "spotify:track:" + savedTrack)
                .toList();

        addTracksToPlaylist(playlistId, uris);

        Image image;

        try {
            if (artist.length == 1) {
                image = getBiggestImage(API.searchArtists(artist[0]).build().execute().getItems()[0].getImages());
            } else {
                image = getBiggestImage(API.getTrack(tracks.get(RANDOM.nextInt(tracks.size()))).build().execute().getAlbum().getImages());
            }

            if (API.getPlaylist(playlistId).build().execute().getImages().length != 1 || artist.length != 1) {
                uploadImageToPlaylist(playlistId, image);
            }
        } catch (Exception e) {
            LOGGER.error("Error getting biggest image", e);
        }

        API.changePlaylistsDetails(playlistId).description((checkAll ? "duo-" : "single-") + "playlist: [" + String.join(":", artist) + "]").build().execute();
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

    private static List<String> getTracksWithArtist(boolean checkAll, boolean bypass, String... artist) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
        List<AbstractModelObject> likedSongs;

        List<String> artistSongs = new ArrayList<>();

        if (checkAll) {
            List<AbstractModelObject> objects = new ArrayList<>();
            List<String> reportedAlbums = new ArrayList<>();
            for (String s : artist) {
                String id = API.searchArtists(s).build().execute().getItems()[0].getId();
                for (AlbumSimplified allPagingItem : getAllPagingItems(API.getArtistsAlbums(id))) {
                    if (!reportedAlbums.contains(allPagingItem.getName()) && toString(allPagingItem.getArtists(), ArtistSimplified::getName).contains(s)) {
                        List<String> names = objects
                                .stream()
                                .map(abstractModelObject -> (TrackSimplified) abstractModelObject)
                                .map(TrackSimplified::getName)
                                .toList();
                        if (!names.contains(allPagingItem.getName())) {
                            LOGGER.info("Retrieving tracks from: " + allPagingItem.getName() + " " + toString(allPagingItem.getArtists(), ArtistSimplified::getName));
                            List<TrackSimplified> trackSimplifieds = getTotalEntities(API.getAlbumsTracks(allPagingItem.getId()).build().execute().getTotal(), spotifyApi -> spotifyApi.getAlbumsTracks(allPagingItem.getId()));
                            objects.addAll(trackSimplifieds);
                            reportedAlbums.add(allPagingItem.getName());
                        }
                    }
                }
            }

            likedSongs = objects;
        } else {
            likedSongs = getTotalEntities(API.getUsersSavedTracks().build().execute().getTotal(), SpotifyApi::getUsersSavedTracks)
                    .stream()
                    .map(SavedTrack::getTrack)
                    .map(track -> (AbstractModelObject) track)
                    .toList();
        }

        for (AbstractModelObject savedTrack : likedSongs) {
            if (savedTrack instanceof Track track) {
                if (!bypass) {
                    if (GeniusRequests.isArtistOnSong(track.getName(), track.getArtists()[0].getName(), artist)) {
                        artistSongs.add(track.getId());
                    }
                }

                String artists = toString(track.getArtists(), ArtistSimplified::getName);
                boolean bl = true;
                for (String s : artist) {
                    if (!artists.contains(s)) {
                        bl = false;
                        break;
                    }
                }
                if (bl && !artistSongs.contains(track.getId())) {
                    artistSongs.add(track.getId());
                }
            } else if (savedTrack instanceof TrackSimplified trackSimplified) {
                if (!bypass) {
                    if (GeniusRequests.isArtistOnSong(trackSimplified.getName(), trackSimplified.getArtists()[0].getName(), artist)) {
                        artistSongs.add(trackSimplified.getId());
                    }
                }

                String artists = toString(trackSimplified.getArtists(), ArtistSimplified::getName);
                boolean bl = true;
                for (String s : artist) {
                    if (!artists.contains(s)) {
                        bl = false;
                        break;
                    }
                }
                if (bl && !artistSongs.contains(trackSimplified.getId())) {
                    artistSongs.add(trackSimplified.getId());
                }
            }
        }

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

    public static void deleteOtherWeeklyRotationPlaylists() throws IOException, ParseException, SpotifyWebApiException {
        for (PlaylistSimplified totalEntity : getTotalEntities(API.getListOfCurrentUsersPlaylists().build().execute().getTotal(), SpotifyApi::getListOfCurrentUsersPlaylists)) {
            if (totalEntity.getName().contains("Weekly Rotation")) {
                API.unfollowPlaylist(totalEntity.getId()).build().execute();
            }
        }
    }

    public static void executeWeeklyRotation() throws IOException, ParseException, SpotifyWebApiException {
        deleteOtherWeeklyRotationPlaylists();

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

        AlbumSimplified albumSimplified = getAlbumStatistics(weeklyRotation.stream().map(SavedTrack::getTrack).toList()).entrySet().stream().toList().get(0).getKey();

        Image image = getBiggestImage(albumSimplified.getImages());

        uploadImageToPlaylist(playlistId, image);

        LOGGER.info("https://open.spotify.com/playlist/" + playlistId);
    }

    private static Map<AlbumSimplified, Number> getAlbumStatistics(List<Track> tracks) {
        Map<AlbumSimplified, Number> map = new LinkedHashMap<>();

        for (Track track : tracks) {
            if (!map.containsKey(track.getAlbum())) {
                map.put(track.getAlbum(), 1);
            } else {
                map.replace(track.getAlbum(), map.get(track.getAlbum()).intValue() + 1);
            }
        }

        map = sortArtistMap(map);

        return map;
    }

    private static <T> List<T> getAllPagingItems(AbstractDataPagingRequest.Builder<T, ?> requestBuilder) throws IOException, ParseException, SpotifyWebApiException {
        List<T> list = new ArrayList<>();

        int total = requestBuilder.build().execute().getTotal();

        for (int i = 0; i < total; i += 50) {
            list.addAll(List.of(requestBuilder
                    .offset(i)
                    .limit(50)
                    .build()
                    .execute()
                    .getItems()));
        }
        return list;
    }


    private static void deleteAllSongsInPlaylist(String playlistId) throws IOException, ParseException, SpotifyWebApiException {
        List<String> array = new ArrayList<>();

        for (PlaylistTrack item : getAllPagingItems(API.getPlaylistsItems(playlistId))) {
            String id = item.getTrack().getId();

            if (item.getIsLocal()) {
                continue;
            }

            array.add("spotify:track:" + id);
        }

        for (List<String> subListX : subListX(array, 100)) {
            JsonArray object = new JsonArray();
            subListX.forEach(s -> {
                JsonObject object1 = new JsonObject();
                object1.addProperty("uri", s);
                object.add(object1);
            });

            API.removeItemsFromPlaylist(playlistId, object).build().execute();
        }
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

        for (int i = 0; i < GENERAL_CONFIG.getConfigOption("recent-songs-limit", Integer::parseInt); i++) {
            weeklyRotation.add(recentTracks.get(RANDOM.nextInt(recentTracks.size())));
        }

        int currentSize = weeklyRotation.size();

        for (int i = 0; i < (GENERAL_CONFIG.getConfigOption("weekly-rotation-song-limit", Integer::parseInt) - currentSize); i++) {
            weeklyRotation.add(savedTracks.get(RANDOM.nextInt(savedTracks.size())));
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

    public static <T> List<T> getTotalEntities(int total, Function<SpotifyApi, ? extends AbstractDataPagingRequest.Builder<T, ?>> function) throws IOException, ParseException, SpotifyWebApiException {
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
