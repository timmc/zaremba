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
java -jar target/zaremba-*-jar-with-dependencies.jar latex --z records-z.txt --v records-v.txt
```

TODO:

- Comb through for any remaining locations where an overflow could occur, or just switch to BigInteger


New approach:


Run the program for a large maxN:
- Precompute:
    - The list of primorials `pr(k) < n: [2, 6, 30...]`
    - For each primorial k, make a list of its usable exponents, `[0, ceil(log_k(maxN))]`
    - The powers of the primes represented in the list of primorials. If 4 primorials were used, then this would be `2^[0, 4], 3^[0, 3], 5^[0, 2], 7^[0, 1]`
    - ln() of those powers of primes
- Make the waterfall numbers <= n, plus some extra:
    - Take the Cartesian product of all the usable primorial exponent lists: [1, 2, 4 ...] × [1, 6, 36, 216 ...] × ...
    - Each element of the Cartesian product will represent a waterfall number as some product of primorials, with repetition, from [1, 1, 1, 1] up to [128, 216, 900, 210].
    - In code, these will just be the exponent of each primorial, with possible values [0, 0, 0, 0] up to [7, 3, 2, 1].
    - For example, waterfall number [4, 3, 1, 0] would be 2^4 * 6^3 * 30^1 * 210^0
- For each waterfall number (using [4, 3, 1, 0] as an example), compute:
    - n, by multiplying the precomputed primorial powers: `16 * 216 * 30`
    - The prime exponents: `[4+3+1, 3+1, 1] = [8, 4, 1] = 2^8 * 3^4 * 5^1`
    - tau(n), from the prime exponents: `(8 + 1) * (4 + 1) * (1 + 1)`
    - z(n):
        - Take another Cartesian product of the possible prime exponents to represent divisors: [0, 8] × [0, 4] × [0, 1]
        - For each divisor look up its value and its ln() in the precomputed lists, divide and sum as appropriate
    - v(n)
- Sort the waterfall numbers by n and find z and v records
