package com.gr8di.lms

import com.gr8di.lms.database.DatabaseVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class)

    @Override
    public void start(Promise<Void> promise) throws Exception {
        Promise<String> dbVerticleDeployment = Promise.promise()
        vertx.deployVerticle(new DatabaseVerticle(), dbVerticleDeployment)

        dbVerticleDeployment.future().compose({ id ->

            Promise<String> gatewayVerticleDeployment = Promise.promise()
            vertx.deployVerticle(
                    "com.gr8di.lms.gateway.GatewayVerticle",
                    new DeploymentOptions().setInstances(2),
                    gatewayVerticleDeployment);

            return gatewayVerticleDeployment.future()

        }).setHandler({ ar ->
            if (ar.succeeded()) {
                promise.complete()
            } else {
                promise.fail(ar.cause())
            }
        })
    }
}
