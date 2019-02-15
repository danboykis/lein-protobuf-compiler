# Changes from lein-protobuf

### Forked to make it work with newer versions of other libraries

This is a stripped-down version of [lein-protobuf](https://github.com/flatland/lein-protobuf). It behaves the same
as lein-protobuf, with the following exceptions:

* It cannot download and compile the Google protocol buffer
  compiler for you. You must have protoc on your path.
* It can support multiple include directories. Specify these in
  your project as `:protobuf-includes ["patha" "pathb" "pathc"]`
  

