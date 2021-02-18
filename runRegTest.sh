#!/bin/sh
export BTCPROXYD_VERSION=0.1.2
export CONFIG_FILE=regtest-proxy.yml
export CONFIG_DIR=/etc/btcproxyd
export MICRONAUT_CONFIG_FILES=${CONFIG_DIR}/${CONFIG_FILE}
docker run -e MICRONAUT_CONFIG_FILES \
    --mount type=bind,source=$PWD/config,dst=${CONFIG_DIR} \
    -p8080:8080 \
    consensusj/btcproxyd:${BTCPROXYD_VERSION}

