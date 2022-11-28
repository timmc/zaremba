A Kotlin version.

Build:

```bash
mvn package
```

Calculate records for z(n) for 5 minutes:

```bash
timeout 5m java -jar target/zaremba-*-jar-with-dependencies.jar records z
```

Continue the search for a higher V based on checkpoint in `continue-v.json`:

```bash
ZAREMBA_CONTINUE=continue-v.json java -jar target/zaremba-*-jar-with-dependencies.jar records v | tee -a records-v.txt
```

Combine Z and V logs for LaTeX output:

```bash
java -jar target/zaremba-*-jar-with-dependencies.jar latex records-z.txt records-v.txt
```

TODO:

- Comb through for any remaining locations where an overflow could occur
