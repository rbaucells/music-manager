package rbaucells.MusicManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
