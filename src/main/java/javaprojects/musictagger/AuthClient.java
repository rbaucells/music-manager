package javaprojects.musictagger;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class AuthClient {
    public enum CodeChallengeType {
        S256,
        Plain
    }

    String clientId;
    HttpHandler httpHandler;
    CodeChallengeType codeChallengeType = CodeChallengeType.S256;
    String redirectPath;
    int redirectPort;
    URI authorizationURI;
    String scope = "";

    public AuthClient() { }

    public AuthClient(String clientId, HttpHandler httpHandler, int redirectPort, String redirectPath, URI authorizationURI) {
        this.clientId = clientId;
        this.httpHandler = httpHandler;
        this.redirectPort = redirectPort;
        // Apply the same logic for redirectPath as in the setter
        if (!redirectPath.startsWith("/")) {
            this.redirectPath = "/" + redirectPath;
        } else {
            this.redirectPath = redirectPath;
        }
        this.authorizationURI = authorizationURI;
    }

    public AuthClient(String clientId, HttpHandler httpHandler, CodeChallengeType codeChallengeType, int redirectPort, String redirectPath, URI authorizationURI, String scope) {
        this.clientId = clientId;
        this.httpHandler = httpHandler;
        this.codeChallengeType = codeChallengeType;
        this.redirectPort = redirectPort;
        // Apply the same logic for redirectPath as in the setter
        if (!redirectPath.startsWith("/")) {
            this.redirectPath = "/" + redirectPath;
        } else {
            this.redirectPath = redirectPath;
        }
        this.authorizationURI = authorizationURI;
        this.scope = scope;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public void setHttpHandler(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }
    public void setCodeChallengeType(CodeChallengeType codeChallengeType) {
        this.codeChallengeType = codeChallengeType;
    }
    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
    }
    public void setRedirectPath(String redirectPath) {
        if (redirectPath.startsWith("/"))
            this.redirectPath = redirectPath;
        else
            this.redirectPath = "/" + redirectPath;
    }
    public void setAuthorizationURI(URI authorizationURI) {
        this.authorizationURI = authorizationURI;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }


    public String getClientId() {
        return clientId;
    }
    public HttpHandler getHttpHandler() {
        return httpHandler;
    }
    public CodeChallengeType getCodeChallengeType() {
        return codeChallengeType;
    }
    public int getRedirectPort() {
        return redirectPort;
    }
    public String getRedirectPath() {
        return redirectPath;
    }
    public URI getAuthorizationURI() {
        return authorizationURI;
    }
    public String getScope() {
        return scope;
    }

    public void RequestUserAuthorization() throws NoSuchAlgorithmException, URISyntaxException, IOException {
        String codeChallenge = createCodeChallenge(64);

        String query = "";

        query += "response_type=code";
        query += "&client_id=" + clientId.strip();
        if (scope != null)
            query += "&scope=" + scope.strip();

        switch (codeChallengeType) {
            case S256 -> query += "&code_challenge_method=S256";
            case Plain -> query += "&code_challenge_method=plain";
        }

        query += "&code_challenge=" + codeChallenge;
        query += "&redirect_uri=http://127.0.0.1:" + redirectPort + redirectPath;

        URI uri = new URI(authorizationURI.getScheme(), authorizationURI.getHost(), authorizationURI.getPath(), query, authorizationURI.getFragment());

        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", redirectPort), 0);
        httpServer.createContext(redirectPath, httpHandler);
        httpServer.setExecutor(null);
        httpServer.start();

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            }
        }
    }

    public static void RequestUserAuthorization(String clientId, HttpHandler httpHandler, CodeChallengeType codeChallengeType, int redirectPort, String redirectPath, URI authorizationURI, String scope) throws NoSuchAlgorithmException, URISyntaxException, IOException {
        String codeChallenge = createCodeChallenge(64, codeChallengeType);

        String query = "";

        query += "response_type=code";
        query += "&client_id=" + clientId.strip();
        if (scope != null)
            query += "&scope=" + scope.strip();

        switch (codeChallengeType) {
            case S256 -> query += "&code_challenge_method=S256";
            case Plain -> query += "&code_challenge_method=plain";
        }

        query += "&code_challenge=" + codeChallenge;
        query += "&redirect_uri=http://127.0.0.1:" + redirectPort + redirectPath;

        URI uri = new URI(authorizationURI.getScheme(), authorizationURI.getHost(), authorizationURI.getPath(), query, authorizationURI.getFragment());

        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", redirectPort), 0);
        httpServer.createContext(redirectPath, httpHandler);
        httpServer.setExecutor(null);
        httpServer.start();

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            }
        }
    }

    public String createCodeChallenge(int verifierSize) throws NoSuchAlgorithmException {
        if (verifierSize < 43 || verifierSize > 128) {
            throw new RuntimeException("verifierSize was invalid. verifierSize must be between 43 and 128");
        }
        switch (codeChallengeType) {
            case S256 -> {
                String codeVerifier = createCodeVerifier(verifierSize);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
                String codeChallenge = Base64.getUrlEncoder().encodeToString(hash);
                return codeChallenge.replace("=", "").replace("+", "-").replace("/", "_");
            }
            case Plain -> {
                return createCodeVerifier(verifierSize);
            }
        }

        return "";
    }

    public static String createCodeChallenge(int verifierSize, CodeChallengeType codeChallengeType) throws NoSuchAlgorithmException {
        if (verifierSize < 43 || verifierSize > 128) {
            throw new RuntimeException("verifierSize was invalid. verifierSize must be between 43 and 128");
        }
        switch (codeChallengeType) {
            case S256 -> {
                String codeVerifier = createCodeVerifier(verifierSize);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
                String codeChallenge = Base64.getUrlEncoder().encodeToString(hash);
                return codeChallenge.replace("=", "").replace("+", "-").replace("/", "_");
            }
            case Plain -> {
                return createCodeVerifier(verifierSize);
            }
        }

        return "";
    }

    public static String createCodeVerifier(int verifierSize) {
        final char[] possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_.-~".toCharArray();

        Random random = new Random();
        String verifier = "";

        for (int i = 0; i < verifierSize; i++) {
            verifier += possibleCharacters[random.nextInt(0, possibleCharacters.length)];
        }

        return verifier;
    }
}
