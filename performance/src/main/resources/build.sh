
SRC_DIR=../java
CODE_DIR=io/landerlyoung/github/kproto/performance/serialtest

mkdir -p $SRC_DIR/$CODE_DIR

rm -r java javalite wire
mkdir java javalite wire

# sed -i "" -e s/serialtest.wire/serialtest.java/g proto.proto
# protoc --java_out=java proto.proto
# rm -r $SRC_DIR/$CODE_DIR/java
# mv -f java/$CODE_DIR/java $SRC_DIR/$CODE_DIR/java


# javalite download from https://repo1.maven.org/maven2/com/google/protobuf/protoc-gen-javalite/3.0.0/
# https://github.com/protocolbuffers/protobuf/blob/master/java/lite.md
sed -i "" -e s/serialtest.wire/serialtest.javalite/g proto.proto
protoc --plugin=$HOME/Downloads/protoc-gen-javalite --javalite_out=javalite proto.proto
rm -r $SRC_DIR/$CODE_DIR/javalite
mv -f javalite/$CODE_DIR/javalite/ $SRC_DIR/$CODE_DIR/

# wire
# wire download from https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-compiler&c=jar-with-dependencies&v=LATEST
# https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-compiler&c=jar-with-dependencies&v=2.2.0
# https://github.com/square/wire
sed -i "" -e s/serialtest.javalite/serialtest.wire/g proto.proto
java -jar ~/Downloads/wire-compiler-2.2.0-jar-with-dependencies.jar  --java_out=wire --proto_path=./ proto.proto
rm -r $SRC_DIR/$CODE_DIR/wire
mv -f wire/$CODE_DIR/wire $SRC_DIR/$CODE_DIR/

rm -r java javalite wire
