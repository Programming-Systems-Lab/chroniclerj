#!/bin/sh
jar cmf META-INF/MANIFEST.MF chroniclerj.jar META-INF/ listenerMethods.txt nondeterministic-methods.txt -C bin .
