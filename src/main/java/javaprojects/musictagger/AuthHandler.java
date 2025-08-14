package javaprojects.musictagger;

import ch.qos.logback.core.joran.spi.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Consumer;

public class AuthHandler implements HttpHandler {
    private final Consumer<HttpExchange> handleConsumer;

    public AuthHandler(Consumer<HttpExchange> handleConsumer) {
        this.handleConsumer = handleConsumer;
    }

    public void handle(HttpExchange exchange) {
        handleConsumer.accept(exchange);
    }
}
