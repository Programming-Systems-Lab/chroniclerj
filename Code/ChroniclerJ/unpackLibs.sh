#!/bin/sh
cp lib/asm-all-4.0.jar bin/; cp lib/kxml2-2.3.0.jar bin/; cp lib/log4j-1.2.16.jar bin/; cp lib/objenesis-1.2.jar bin/; cp lib/xstream-1.4.2.jar bin/;
cd bin
unzip -n asm-all-4.0.jar; unzip -n kxml2-2.3.0.jar; unzip -n log4j-1.2.16.jar; unzip -n objenesis-1.2.jar; unzip -n xstream-1.4.2.jar
rm asm-all-4.0.jar; rm kxml2-2.3.0.jar; rm log4j-1.2.16.jar; rm objenesis-1.2.jar; rm xstream-1.4.2.jar; rm -rf META-INF/
