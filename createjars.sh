# Client first
rm -f GMCClient.jar
rm -f GMCClient.zip
find ./GenericMultiplayerClient/ -name "*.java" > sources.txt
find ./GenericMultiplayerShared/ -name "*.java" >> sources.txt
rm -rf bin
mkdir bin
javac -source 1.5 -d bin @sources.txt $*
jar cvfm GMCClient.jar client_manifest.txt -C bin .
zip GMCClient GMCClient.jar readme.md gpl-3.0.txt
rm sources.txt
# Server
rm -f GMCServer.jar
rm -f GMCServer.zip
find ./GenericMultiplayerServer/ -name "*.java" > sources.txt
find ./GenericMultiplayerShared/ -name "*.java" >> sources.txt
rm -rf bin
mkdir bin
javac -source 1.5 -d bin @sources.txt $*
jar cvfm GMCServer.jar server_manifest.txt -C bin .
zip GMCServer GMCServer.jar readme.md gpl-3.0.txt
rm sources.txt
rm -rf bin

