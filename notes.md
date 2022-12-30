fixed k -> finite list of candidates n (exponent patterns)

inputs:
- k = 29
- V = 1.7059578102443238

alg:
#. Use Weber's Lemma ("Lemma 2. (Lemma 4.3 from [16])" from Weighted pdf) -- p is distinct
   prime factors -- to get an upper bound on z(n). Name this zmax for this iteration.
#. Max bound on log tau = zmax/V (name it maxlt)
#. Find all exponent combinations for first k primes where:
    - All exponents >= 1
    - Waterfall number
    - logtau <= maxlt

Like starting waterfall search with kth primorial to first power

0 0 0 ... 1 (primorials)

1 0 0 ... 1
2 0 0 ... 1
3 0 0 ... 1
...

2 1 0 ... 1


logtau creates a bound on upward movement, but not a predictable one on rightward movement


...finally get a list of candidate n (as primorial exponents). Check if any of them are new record-setters for v(n).

----

then try the next lower k

----

start at k = 54 just for funsies


----

tighten waterfall sequence generator with lower bounds?
