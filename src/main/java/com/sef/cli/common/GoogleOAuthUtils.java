package com.sef.cli.common;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.sef.cli.common.properties.GoogleOauthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleOAuthUtils {

    private static GoogleOauthProperties googleOauthProperties;

    @Autowired
    public void setGoogleOauthProperties(GoogleOauthProperties properties) {
        GoogleOAuthUtils.googleOauthProperties = properties;
    }

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static GoogleTokenResponse getTokenByCode(String code) throws IOException {
        return new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                googleOauthProperties.getClientId(),
                googleOauthProperties.getClientSecret(),
                code,
                googleOauthProperties.getRedirectUri()
        ).execute();
    }

    public static GoogleIdToken.Payload verifyIdToken(String idTokenString) {
        try {
            GoogleIdToken idToken = GoogleIdToken.parse(JSON_FACTORY, idTokenString);

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
                    .setAudience(Collections.singletonList(googleOauthProperties.getClientId()))
                    .build();

            if (verifier.verify(idToken)) {
                return idToken.getPayload();
            } else {
                System.err.println("idToken 驗證失敗");
                return null;
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
