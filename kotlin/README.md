A Kotlin version.

Build:

```bash
mvn package
```

Calculate records for z(n) and v(n) up to n=1e19:

```bash
timeout 5m java -jar target/zaremba-*-jar-with-dependencies.jar records 10000000000000000000 | tee -a records.json
```

Reformat records for LaTeX table output:

```bash
java -jar target/zaremba-*-jar-with-dependencies.jar latex records.json > records.latex
```

Use k-primes/max-tau approach instead, with highest known v(n) record as input:

```bash
for k in {1..35}; do echo "Running with k=$k"; java -jar target/zaremba-*-jar-with-dependencies.jar k-primes --V 1.7059578102443238 --k $k; echo; echo; done | tee kprimes.txt
```

TODO:

- Parallelize
- Look into floating point error (maybe can detect if a record would be within
  the margin of error)
