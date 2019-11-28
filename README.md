# lein-protobuf-compiler

```
[com.danboykis/lein-protobuf-compiler "0.0.2"]
```

This is a stripped-down version of [lein-protobuf](https://github.com/flatland/lein-protobuf).
The idea for this project was procured from [lein-protobuf-minimal-mg](https://github.com/markusgustavssonking/lein-protobuf-minimal-mg/).
It is similar to lein-protobuf. The most notable difference is that it cannot download and compile the
Google protocol buffer compiler for you. You must have protoc installed.

* It can support multiple include directories. Specify these in your project as `:protobuf-includes ["patha" "pathb" "pathc"]`
* It can support multiple proto directories `:proto-paths ["resources/protoA" "protoB"]`
* If protoc is not in your path specify it via `:protoc "/path/to/your/protoc"`
  
To invoke it simply run

```
lein protobuf
```

For an example project see: example directory with person-proto project in this repo.
