#!/bin/sh
export BTCPROXY_VERSION=`grep btcProxyVersion gradle.properties | cut -d'=' -f2 | xargs`
export CONFIG_FILE=proxy-regtest.yml
export CONFIG_DIR=/etc/btcproxyd
export MICRONAUT_CONFIG_FILES=${CONFIG_DIR}/${CONFIG_FILE}
docker run -e MICRONAUT_CONFIG_FILES \
    --mount type=bind,source=$PWD/config,dst=${CONFIG_DIR} \
    -p8080:8080 \
    consensusj/btcproxyd:${BTCPROXY_VERSION}

