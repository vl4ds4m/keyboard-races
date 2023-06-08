#!/usr/bin/env bash

export JFX=~/javafx-sdk-17.0.7/lib

java --module-path client-jars:$JFX \
--module org.vl4ds4m.keyboardraces.client/org.vl4ds4m.keyboardraces.client.Main