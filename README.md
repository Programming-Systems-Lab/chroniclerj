ChroniclerJ [Chronicler implemented in Java]
=======

*Lightweight Recording of Nondeterministic Inputs to Reproduce Field Failures*

**ChroniclerJ** is a system for recording buggy program executions in the field and reproducing them in the lab. **ChroniclerJ** is our Java implementation of **Chronicler**, a generalized record and replay approach for VM-based languages.

This repository contains the source and a [runnable binary](https://github.com/Programming-Systems-Lab/chroniclerj/blob/master/chroniclerj-0.4.1.jar) for **ChroniclerJ**.

###For more information
Please see our Technical Report:
<http://mice.cs.columbia.edu/getTechreport.php?techreportID=1512&format=pdf>, or email [Jonathan Bell](mailto:jbell@cs.columbia.edu)

Running
-----
### Instrumenting your code
Before deploying your code, you need to run the **ChroniclerJ** instrumenter, which will produce two copies of your program: one for deployment (with logging code), and one for replay purposes (with code that reads back from the log).

To run the instrumenter, use the command `java -jar chroniclerj-0.4.1.jar -instrument {-mxLogN} input ouputLocationDeploy outputLocationReplay`, where:
* `{-mxLogN}` is an optional parameter indiciating that at most `N` items should be stored in the in-memory logfile before it is flushed to disk
* `input` is the path that contains the .class files, .jar files, etc that need to be instrumented (e.g. what you would be deploying normally)
* `outputLocationDeploy` is where you would like the generated code (for deployment) to be placed
* `outputLocationReplay` is where you would like the generated code (for running replays) to be placed

It is important that *all* libraries that you plan to deploy with your application are instrumented. We suggest running the instrumenter at the top level folder that you were going to deploy, which might contain several jar files, several class files, etc.

### Collecting test cases
Deploy your code as you normally would, but deploy the code that was instrumented and placed in `outputLocationDeploy` rather than your original, uninstrumented code. Make sure that the **ChroniclerJ** jar is in your classpath too.

When an uncaught exception ocurrs, **ChroniclerJ** generates a test case. You can manually invoke this process (e.g. from your own exception handler) by calling the method `ChroniclerJExceptionRunner.genTestCase()`. Users are notified that a test case was generated, which is placed in the current working directory and has the name format chroniclerj-crash-*currenttime*.test. The test case file contains all logs necessary to replay the execution.

### Replaying test cases
To replay the failed executions, run the command `java -jar chroniclerj-0.4.1.jar -replay testCase {classpath}`, where
* `testCase` is the test case to run
* `{classpath}` is a space-delimited classpath passed to your program when it starts to replay

Building
-----
**ChroniclerJ** is an eclipse project, and should simply be added to eclipse, and built as a Java project.

###Packaging
To create the **ChroniclerJ** jar, first run the `unpackLibs.sh` command to unpack the libraries that **ChroniclerJ** depends on, then `makeJar.sh` to generate a jar file with **ChroniclerJ** and its libraries embedded.

License
------
This software is released under the MIT license.

Copyright (c) 2012, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


Acknowledgements
-----
This project makes use of the following libraries:
* [ASM](http://asm.ow2.org/license.html), (c) 2000-2011 INRIA, France Telecom, [license](http://asm.ow2.org/license.html)
* [Cloning](https://code.google.com/p/cloning/), (c) 2009 Konstantinos Kougios, released under the Apache License 2.0
* [KXML2](http://kxml.sourceforge.net/kxml2/), (c) 1999-2005, Stefan Haustein, released under the BSD license
* [Objenesis](http://code.google.com/p/objenesis/), © 2006-2012, Joe Walnes, Henri Tremblay and Leonardo Mesquita, released under the Apache License 2.0
* [Log4j](http://logging.apache.org/log4j/), (c) 1999-2012, Apache Software Foundation, released under the Apache License 2.0
* [XStream](http://xstream.codehaus.org/, (c) 2003-2006, Joe Walnes and (c) 2006-2009, 2011 XStream Committers, released under [a BSD license](http://xstream.codehaus.org/license.html)

The authors of this software are [Jonathan Bell](http://jonbell.net), [Nikhil Sarda](https://github.com/diffoperator/) and [Gail Kaiser](http://www.cs.columbia.edu/~kaiser/). The authors are members of the [Programming Systems Laboratory](http://www.psl.cs.columbia.edu/), funded in part by NSF CCF-1161079, NSF CNS-0905246 and NIH 2 U54 CA121852-06.
