package de.intektor.pixelshooter_main_server.net.server;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.Collections;

/**
 * @author Intektor
 */
public class TokenVerifier {

    public static GoogleIdToken isTokenLegit(String userIdToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList("775862664818-4ejmh7ap6nkvjethc6k0l7ond182bj01.apps.googleusercontent.com"))
                .setIssuer("https://accounts.google.com")
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(userIdToken);
        } catch (Throwable t) {
            return null;
        }
        return idToken;
    }
}
