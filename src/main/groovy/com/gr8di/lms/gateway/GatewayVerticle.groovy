package com.gr8di.lms.gateway

import com.gr8di.lms.handlers.HealthCheckHandler
import com.gr8di.lms.handlers.page.HomePageHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine

class GatewayVerticle extends AbstractVerticle{

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayVerticle.class)

    public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port"
    public static final String CONFIG_DB_QUEUE = "db.queue"

    private String dbQueue = "db.queue";

    @Override
    public void start(Promise<Void> promise) throws Exception {

        dbQueue = config().getString(CONFIG_DB_QUEUE, "db.queue")

        def server = vertx.createHttpServer()
        def templateEngine = FreeMarkerTemplateEngine.create(vertx);

        def router = Router.router(vertx)
        router.route("/static/*").handler(StaticHandler.create())

        // site endpoint
        router.mountSubRouter("/site", siteRoutes(templateEngine));
        // health check endpoint
        router.get("/health").handler(new HealthCheckHandler())

        int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8888);

        server.requestHandler(router)
                .listen(8888, { ar ->
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port " + portNumber)
                        promise.complete()
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause())
                        promise.fail(ar.cause())
                    }
                });
    }

    private Router siteRoutes(def templateEngine) {
        LOGGER.debug("Mounting '/site' endpoint");

        def router = Router.router(vertx)
        router.get("/home").handler(new HomePageHandler(templateEngine))

        return router
    }
}
