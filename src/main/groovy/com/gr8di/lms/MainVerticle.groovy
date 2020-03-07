package com.gr8di.lms

import com.gr8di.lms.handlers.HealthCheckHandler
import com.gr8di.lms.handlers.ResourceNotFoundHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpHeaders


class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class)

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Future<Void> steps = prepareDatabase().compose({ v -> startHttpServer() })
        steps.setHandler(startFuture.completer())
    }

    private Future<Void> prepareDatabase() {
        Future<Void> future = Future.future()
//        initDB();
//        if(dbClient != null){
//            future.complete()
//        }
        future.complete()
        return future;
    }

    private Future<Void> startHttpServer() {
        Future<Void> future = Future.future()

        HttpServer server = vertx.createHttpServer()

        LOGGER.debug("in mainVerticle.start(..)")
        Router router = Router.router(vertx);
        router.route()
                .handler(CorsHandler.create("*")
                                .allowedMethod(HttpMethod.GET)
                                .allowedMethod(HttpMethod.POST)
                                .allowedMethod(HttpMethod.PUT)
                                .allowedMethod(HttpMethod.OPTIONS)
                                .allowedHeader(HttpHeaders.ACCEPT.toString())
                                .allowedHeader(HttpHeaders.CONTENT_TYPE.toString()))

        // Decode body of all requests
        router.route().handler(BodyHandler.create())

        // Health Check
        router.get("/health")
                .handler(new HealthCheckHandler())

        // Error
        router.route()
                .last()
                .handler(new ResourceNotFoundHandler())

        server.requestHandler(router)
                .listen(8888, { ar ->
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port 8888")
                        future.complete()
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause())
                        future.fail(ar.cause())
                    }
                });
        return future;
    }
}
