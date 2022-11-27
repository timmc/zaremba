A Kotlin version.

Build, and calculate records for z(n) and v(n) for a minute each:

```bash
mvn package
timeout 1m time java -jar target/zaremba-*-jar-with-dependencies.jar records v | tee records-v.txt
timeout 1m time java -jar target/zaremba-*-jar-with-dependencies.jar records z | tee records-z.txt
```

TODO:

- Comb through for any remaining locations where an overflow could occur
- Split z and v record finders (and recalc vStep on a schedule)
