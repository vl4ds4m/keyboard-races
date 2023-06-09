#!/usr/bin/env bash

export JFX=~/javafx-sdk-17.0.7/lib

java --module-path server-jars \
--module org.vl4ds4m.keyboardraces.server/org.vl4ds4m.keyboardraces.server.Server
