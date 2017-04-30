#!/usr/bin/env bash
mvn -pl tf  -Dmaven.test.skip  exec:java -Dexec.mainClass="org.openchai.tensorflow.TfServer"
