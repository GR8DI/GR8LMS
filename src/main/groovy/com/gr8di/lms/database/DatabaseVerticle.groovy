package com.gr8di.lms.database

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLConnection

class DatabaseVerticle extends AbstractVerticle {

    public static final String CONFIG_DB_JDBC_URL = "db.jdbc.url"
    public static final String CONFIG_DB_JDBC_DRIVER_CLASS = "db.jdbc.driver_class"
    public static final String CONFIG_DB_JDBC_MAX_POOL_SIZE = "db.jdbc.max_pool_size"
    public static final String CONFIG_DB_SQL_QUERIES_RESOURCE_FILE = "db.sqlqueries.resource.file"

    public static final String CONFIG_DB_QUEUE = "db.queue"

    private JDBCClient dbClient

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class)

    private enum SqlQuery {
        CREATE_COURSE_TABLE,
        GET_COURSE_BY_ID,
        CREATE_COURSE,
        UPDATE_COURSE,
        GET_ALL_COURSES,
        DELETE_COURSE
    }

    private final HashMap<SqlQuery, String> sqlQueries = new HashMap<>();

    private void loadSqlQueries() throws IOException {

        String queriesFile = config().getString(CONFIG_DB_SQL_QUERIES_RESOURCE_FILE);
        InputStream queriesInputStream;
        if (queriesFile != null) {
            queriesInputStream = new FileInputStream(queriesFile);
        } else {
            queriesInputStream = getClass().getResourceAsStream("/db-queries.properties");
        }

        Properties queriesProps = new Properties();
        queriesProps.load(queriesInputStream);
        queriesInputStream.close();

        sqlQueries.put(SqlQuery.CREATE_COURSE_TABLE, queriesProps.getProperty("create-course-table"));
        sqlQueries.put(SqlQuery.GET_ALL_COURSES, queriesProps.getProperty("get-all-courses"));
        sqlQueries.put(SqlQuery.GET_COURSE_BY_ID, queriesProps.getProperty("get-course-by-id"));
        sqlQueries.put(SqlQuery.CREATE_COURSE, queriesProps.getProperty("create-course"));
        sqlQueries.put(SqlQuery.UPDATE_COURSE, queriesProps.getProperty("update-course"));
        sqlQueries.put(SqlQuery.DELETE_COURSE, queriesProps.getProperty("delete-course"));
    }

    @Override
    void start(Promise<Void> promise) throws Exception {
        LOGGER.debug("deploying database verticle ...")

        //TODO: use executeBlocking
        loadSqlQueries()

        dbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getString(CONFIG_DB_JDBC_URL, "jdbc:hsqldb:file:db/gr8lms"))
                .put("driver_class", config().getString(CONFIG_DB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
                .put("max_pool_size", config().getInteger(CONFIG_DB_JDBC_MAX_POOL_SIZE, 30)))

        dbClient.getConnection({ ar ->
            if (ar.failed()) {
                LOGGER.error("Could not open a database connection", ar.cause());
                promise.fail(ar.cause());
            } else {
                SQLConnection connection = ar.result();
                connection.execute(sqlQueries.get(SqlQuery.CREATE_COURSE_TABLE), { create ->
                    connection.close();
                    if (create.failed()) {
                        LOGGER.error("Database preparation error", create.cause())
                        promise.fail(create.cause())
                    } else {
                        vertx.eventBus().consumer(config().getString(CONFIG_DB_QUEUE, "db.queue"), this.&onMessage)
                        promise.complete()
                    }
                });
            }
        });
    }

    enum ErrorCodes {
        NO_ACTION_SPECIFIED,
        BAD_ACTION,
        DB_ERROR
    }

    void onMessage(Message<JsonObject> message) {

        if (!message.headers().contains("action")) {
            LOGGER.error("No action header specified for message with headers {} and body {}",
                    message.headers(), message.body().encodePrettily());
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
            return;
        }
        String action = message.headers().get("action");

        switch (action) {
            case "get-all-courses":
                fetchAllCourses(message)
                break
            default:
                message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
        }
    }

    private void fetchAllCourses(Message<JsonObject> message) {
        dbClient.query(sqlQueries.get(SqlQuery.GET_ALL_COURSES), { queryResult ->
            if (queryResult.succeeded()) {
                List courses = []
                queryResult.result().results.each { json -> courses << json.getString(0)}
                message.reply(new JsonObject().put("courses", new JsonArray(courses)))
            } else {
                reportQueryError(message, queryResult.cause())
            }
        })
    }

    private void reportQueryError(Message<JsonObject> message, Throwable cause) {
        LOGGER.error("Database query error", cause)
        message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage())
    }
}
