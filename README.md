# Zero-Knowledge Treasury on Top of [ERGO](https://ergoplatform.org/en/)
Server-side app for ZK Treasury implemented on top of ERGO's Distributed Signatures.
## Configuration
If you'd like to disable public team creation, then set the teamCreation config in the config file to false.
You can find a sample config file [here](conf/application.conf).

## Running the code
### Development mode
To run the code in development run `sbt "run 9000"` in the main directory to start the server. Load the UI in http://localhost:9000.
replace 9000 with your desired port.
### Jar file
You can download the client's jar file [here](https://github.com/anon-real/DistributedSigsServer/releases). To run the client app:
```bash
java -jar ZKTreasury-client-{version}.jar
# similarly if you want to provide config file:
java -jar -Dconfig.file="path/to/your/config/file" ZKTreasury-client-{version}.jar

```
If you want to run the client on different port also add `-Dhttp.port=8000` and replace 8000 with your desired port.

## Docker Quick Start
TODO

