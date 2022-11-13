A Kotlin version.

```bash
mvn package
java -jar target/zaremba-*-jar-with-dependencies.jar 10000000
```

I'm not sure of the current time complexity, but successive increases
of the max-n by a factor of 10 resulted in successive time increases
by factors of 3, 6, and 10 (the last being from max-n = 1e8 to 1e9).
