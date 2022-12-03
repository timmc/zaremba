A Kotlin version.

Build:

```bash
mvn package
```

Calculate records for z(n) and v(n) for 5 minutes:

```bash
timeout 5m java -jar target/zaremba-*-jar-with-dependencies.jar records | tee -a records.json
```

Reformat records for LaTeX table output:

```bash
java -jar target/zaremba-*-jar-with-dependencies.jar latex records.json > records.latex
```

TODO:

- Parallelize
- Look into floating point error (maybe can detect if a record would be within
  the margin of error)
