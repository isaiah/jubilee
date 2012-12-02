// -*- Mode: java; c-basic-offset: 4 -*- vim:set ft=java sw=4 sts=4:
// $Id$
import org.vertx.java.core.*;
import org.vertx.java.core.http.*;
public class ServerTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("starting...");
        Vertx vertx = Vertx.newVertx();
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response.end("hello world");
                //String file = req.path.equals("/") ? "index.html" : req.path;
                //req.response.sendFile("webroot/" + file);
            }
        }).setSSL(true).setKeyStorePath("../jubilee/server-keystore.jks").setKeyStorePassword("wibble").listen(8080);
        while (true)
            Thread.currentThread().sleep(1);
    }
}
