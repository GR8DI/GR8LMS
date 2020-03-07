package com.gr8di.lms


import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine

class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class)
    private JDBCClient dbClient
    private FreeMarkerTemplateEngine templateEngine

    private static final String SQL_CREATE_COURSE_TABLE = "CREATE TABLE IF NOT EXISTS course (id INTEGER IDENTITY PRIMARY KEY, name VARCHAR(255) UNIQUE, description CLOB)"
    private static final String SQL_GET_COURSE_BY_ID = "SELECT * FROM course WHERE id = ?"
    private static final String SQL_CREATE_COURSE = "INSERT INTO course VALUES (NULL, ?, ?)"
    private static final String SQL_UPDATE_COURSE = "UPDATE course SET name = ?, description = ? WHERE id = ?"
    private static final String SQL_GET_ALL_COURSES = "SELECT * FROM course"
    private static final String SQL_DELETE_COURSE = "DELETE FROM course WHERE id = ?"

    @Override
    public void start(Promise<Void> promise) throws Exception {
        Future<Void> steps = prepareDatabase().compose({ v -> startHttpServer() })
        steps.setHandler(promise)
    }

    private Future<Void> prepareDatabase() {
        Promise<Void> promise = Promise.promise()

        dbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:hsqldb:file:db/gr8lms")
                .put("driver_class", "org.hsqldb.jdbcDriver")
                .put("max_pool_size", 30))

        dbClient.getConnection({ ar ->
            if (ar.failed()) {
                LOGGER.error("Could not open a database connection", ar.cause())
                promise.fail(ar.cause())
            } else {
                SQLConnection connection = ar.result()
                connection.execute(SQL_CREATE_COURSE_TABLE, { create ->
                    connection.close()
                    if (create.failed()) {
                        LOGGER.error("Database preparation error", create.cause())
                        promise.fail(create.cause())
                    } else {
                        promise.complete()
                    }
                })
            }
        })

        return promise.future()
    }


    private Future<Void> startHttpServer() {
        Promise<Void> promise = Promise.promise()
        HttpServer server = vertx.createHttpServer()

        Router router = Router.router(vertx)
        router.route("/static/*").handler(StaticHandler.create())
        router.get("/site/home").handler(this.&homeHandler)
//        router.get("/lms/:course").handler(this.&courseRenderingHandler)
//        router.post().handler(BodyHandler.create())
//        router.post("/update").handler(this.&courseUpdateHandler)
//        router.post("/create").handler(this.&courseCreateHandler)
//        router.post("/delete").handler(this.&courseDeletionHandler)

        templateEngine = FreeMarkerTemplateEngine.create(vertx);

        server
                .requestHandler(router)
                .listen(8888, { ar ->
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port 8888")
                        promise.complete()
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause())
                        promise.fail(ar.cause())
                    }
                });

        return promise.future();
    }

    private void homeHandler(RoutingContext context) {
        context.put("title", "Home")
        templateEngine.render(context.data(), "templates/site/home.ftl", { ar ->
            if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html")
                context.response().end(ar.result())
            } else {
                context.fail(ar.cause())
            }
        })
    }


}
