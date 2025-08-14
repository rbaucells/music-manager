package javaprojects.musictagger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Hashtable;

public class SpotifyAuthHandler implements HttpHandler {
    private final AuthClient authClient;
    private final String codeVerifier;

    private static final byte[] CLOSE_HTML = """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <title>Closing Page</title>
                                <script type="text/javascript">
                                    function closeWindow() {
                                        window.close();
                                    }
                                </script>
                            </head>
                            <body onload="closeWindow()">
                                <p>This page will close automatically.</p>
                            </body>
                        </html>""".getBytes(StandardCharsets.UTF_8);

    public SpotifyAuthHandler(AuthClient authClient) {
        this.authClient = authClient;
        this.codeVerifier = authClient.generateCodeVerifier(64);
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String authCode = getAuthCode(exchange);
        try {
            JSONObject tokensObject = getTokens(authCode);

            String accessToken = tokensObject.getString("access_token");

            int expirationSeconds = tokensObject.getInt("expires_in");

            Application.accessToken = accessToken;
            Application.accessTokenExpiration = Instant.now().plusSeconds(expirationSeconds);

            String refreshToken = tokensObject.getString("refresh_token");

            Application.WriteRefreshTokenToFile("spotify", refreshToken);

            exchange.sendResponseHeaders(200, CLOSE_HTML.length);
            exchange.getResponseHeaders().add("content_type", "text/html");
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(CLOSE_HTML);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    JSONObject getTokens(String authCode) throws URISyntaxException, IOException, InterruptedException {
        String query = "";

        query += "grant_type=authorization_code";
        query += "&code=" + authCode;
        query += "&redirect_uri=" + "http://127.0.0.1:" + authClient.getRedirectPort() + authClient.getRedirectPath();
        query += "&client_id=" + Application.SPOTIFY_CLIENT_ID;
        query += "&code_verifier=" + codeVerifier;

        URI accessTokenURI = new URI("https", null, "accounts.spotify.com", 443, "/api/token", query, null);

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(accessTokenURI)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            return new JSONObject(httpResponse.body());
        }

    }

    String getAuthCode(HttpExchange exchange) {
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        Dictionary<String, String> parameterValue = parseQueryString(query);
        return parameterValue.get("code");
    }

    Dictionary<String, String> parseQueryString(String queryString) {
        String[] seperatedQuery = queryString.split("&");

        Dictionary<String, String> parameterValue = new Hashtable<>();

        for (String string : seperatedQuery) {
            String[] seperatedString = string.split("=");

            parameterValue.put(seperatedString[0], seperatedString[1]);
        }

        return parameterValue;
    }
}
