package com.gr8di.lms.handlers.page

import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine

class HomePageHandler implements Handler<RoutingContext> {
    private final static Logger LOGGER = LoggerFactory.getLogger(HomePageHandler.class)
    private FreeMarkerTemplateEngine templateEngine

    HomePageHandler(FreeMarkerTemplateEngine templateEngine){
        this.templateEngine = templateEngine
    }

    @Override
    void handle(RoutingContext routingContext) {
        routingContext.put("title", "Home")
        templateEngine.render(routingContext.data(), "templates/site/home.ftl", { ar ->
            if (ar.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html")
                routingContext.response().end(ar.result())
            } else {
                routingContext.fail(ar.cause())
            }
        })
    }
}
