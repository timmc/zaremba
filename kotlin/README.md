A Kotlin version.

```bash
mvn package
java -jar target/zaremba-*-jar-with-dependencies.jar records classic
```

TODO:

- Comb through for any remaining locations where an overflow could occur
- Split z and v record finders (and recalc vStep on a schedule)
