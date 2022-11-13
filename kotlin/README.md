A Kotlin version.

```bash
mvn package
java -jar target/zaremba-*-jar-with-dependencies.jar 10000000
```

Current time complexity is unclear and complicated, since it's a bit
step-wise. Successive increases of the max-n by a factor of 10 result
in successive time increases by *very roughly* a factor of 2-3.

It takes about a minute to reach 1e15.
