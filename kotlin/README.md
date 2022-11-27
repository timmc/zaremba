A Kotlin version.

Build:

```bash
mvn package
```

Calculate records for z(n) and v(n) for 5 minutes each:

```bash
timeout 5m java -jar target/zaremba-*-jar-with-dependencies.jar records v | tee records-v.txt
timeout 5m java -jar target/zaremba-*-jar-with-dependencies.jar records z | tee records-z.txt
```

Combine Z and V logs for LaTeX output:

```bash
java -jar target/zaremba-*-jar-with-dependencies.jar latex records-z.txt records-v.txt
```

TODO:

- Comb through for any remaining locations where an overflow could occur
