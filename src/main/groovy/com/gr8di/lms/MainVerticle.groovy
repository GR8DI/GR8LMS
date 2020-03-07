package com.gr8di.lms

import com.gr8di.lms.database.DatabaseVerticle
import com.gr8di.lms.gateway.GatewayVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class)

    @Override
    void start(Promise<Void> promise) throws Exception {
        Promise<String> databaseVerticleDeployment = Promise.promise()
        vertx.deployVerticle(new DatabaseVerticle(), databaseVerticleDeployment)

        databaseVerticleDeployment.future().compose({ id ->

            Promise<String> gatewayVerticleDeployment = Promise.promise()
            vertx.deployVerticle(
                    "com.gr8di.lms.gateway.GatewayVerticle",
                    new DeploymentOptions().setInstances(2),
                    gatewayVerticleDeployment);

            return gatewayVerticleDeployment.future()

        }).setHandler({ ar ->
            if (ar.succeeded()) {
                LOGGER.debug("Verticles deployed successfully")
                promise.complete()
            } else {
                LOGGER.debug("deployment failed" + ar.cause())
                promise.fail(ar.cause())
            }
        })
    }
}
