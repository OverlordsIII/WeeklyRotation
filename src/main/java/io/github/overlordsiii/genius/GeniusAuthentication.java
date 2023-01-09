package io.github.overlordsiii.genius;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.apis.GeniusApi;
import com.github.scribejava.core.builder.ScopeBuilder;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.github.overlordsiii.Main;
import se.michaelthelin.spotify.model_objects.specification.User;

public class GeniusAuthentication {

	private static final String CLIENT_ID = Main.PRIVATE_CONFIG.getConfigOptionNonNull("genius-client-id");
	private static final String CLIENT_SECRET = Main.PRIVATE_CONFIG.getConfigOptionNonNull("genius-client-secret");
	private static final String SECRET_STATE = "100";

	public static final OAuth20Service SERVICE = new ServiceBuilder(CLIENT_ID)
		.apiSecret(CLIENT_SECRET)
		.defaultScope(new ScopeBuilder().withScopes("me", "create_annotation", "manage_annotation", "vote"))
		.callback(Main.PRIVATE_CONFIG.getConfigOptionNonNull("redirect-url"))
		.userAgent("WeeklyRotation")
		.build(GeniusApi.instance());
	public static void buildAuthCodeLink() {
		String authUrl = SERVICE.getAuthorizationUrl(SECRET_STATE);
		Main.LOGGER.info("Authorize App through this link: ");
		Main.LOGGER.info(authUrl);
	}

	public static OAuth2AccessToken retrieveAccessToken() throws IOException, ExecutionException, InterruptedException {
		return SERVICE.getAccessToken(Main.PRIVATE_CONFIG.getConfigOptionNonNull("genius-auth-code"));
	}

	public static void signRequest(OAuthRequest request) {
		SERVICE.signRequest(buildAccessToken(), request);
	}

	public static OAuth2AccessToken buildAccessToken() {
		String token = Main.PRIVATE_CONFIG.getConfigOptionNonNull("genius-access-token");
		String type = Main.PRIVATE_CONFIG.getConfigOptionNonNull("genius-access-token-type");

		return new OAuth2AccessToken(token, type);
	}
}
