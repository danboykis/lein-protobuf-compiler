# Example proto project

In order to start the project do the following

```
lein do clean, protobuf, uberjar
java -jar target/person-runner-uber.jar
```

You should see something like this:

```
Proto to bytes:  [8 4 18 4 66 105 108 108 26 15 98 111 98 64 101 120 97 109 112 108 101 46 99 111 109 34 8 99 108 105 109 98 105 110 103 34 7 114 117 110 110 105 110 103 34 7 106 117 109 112 105 110 103]
Back to proto:  {:id 4, :name Bill, :email bob@example.com, :likes [climbing running jumping]}
```
