# fnflow
Streamed data processing with Java functions as configurable processors using Spring Boot.

## DevEnv Setup
OS: Ubuntu 24.04
native-image (AOT) needs some love to get it working:
- JAVA_HOME envvar must be set for gradle runs. In IDEA, set JAVA_HOME in the execution configuration to the path of the project SDK
- install the gcc build env and some libs needed: `sudo apt-get install build-essential zlib1g-dev`
