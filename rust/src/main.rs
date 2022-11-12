use std::env;
use std::process;
use divisors;

fn zaremba_tau(n: u64) -> (f64, usize) {
    let mut divisors = divisors::get_divisors(n);

    // The divisors crate usually doesn't include 1 or n in the
    // divisors, except for get_divisors(2) == [2]. But the output is
    // sorted, so we can add 1 and n after peeking at the start and
    // end.
    //
    // Look at the high end first because that's where we're going to
    // shove any new values.
    if divisors.len() == 0 || divisors[divisors.len()-1] != n {
        divisors.push(n)
    }
    if divisors.len() == 0 || divisors[0] != 1 {
        divisors.push(1)
    }
    // It's not sorted anymore, but that's OK.

    let tau = divisors.len();
    let mut z = 0_f64;
    for d in divisors {
        let df = d as f64;
        z += df.ln() / df;
    }
    (z, tau)
}

fn do_single(n: u64) {
    let (z, tau) = zaremba_tau(n);
    let ratio = z / (tau as f64).ln();
    println!(
        "z({n}) = {z}\ttau({n}) = {tau}\tz({n}/ln(tau({n})) = {ratio}",
        n = n, z = z, tau = tau, ratio = ratio
    )
}

fn do_records(max_n: u64) {
    let mut record_z = 0.0;
    let mut record_ratio = 0.0;
    for n in 1..max_n {
        let (z, tau) = zaremba_tau(n);
        let ratio = z / (tau as f64).ln();

        let is_record_z = record_z > 0.0 && z > record_z;
        let is_record_ratio = record_ratio > 0.0 && ratio > record_ratio;

        let record_type =
            if is_record_z && is_record_ratio { Some("both")
            } else if is_record_z && !is_record_ratio { Some("z")
            } else if !is_record_z && is_record_ratio { Some("ratio")
            } else { None };
        if let Some(set_records) = record_type {
            println!(
                "{n}\trecord={set_records}\tz({n}) = {z}\ttau({n}) = {tau}\tz({n})/ln(tau({n})) = {ratio}",
                n=n, set_records=set_records, z=z, tau=tau, ratio=ratio
            );
        }

        record_z = record_z.max(z);
        record_ratio = record_ratio.max(ratio);
    }
}

fn die_with_usage() {
    println!("Usage:");
    println!("  ./zaremba single [n]");
    println!("  ./zaremba records [max-n]");
    process::exit(1)
}

fn main() {
    let args: Vec<String> = env::args().collect();

    if args.len() != 3 {
        println!("Wrong number of arguments, expecting 2.");
        die_with_usage()
    }

    match args[1].as_str() {
        "single" => do_single(args[2].parse::<u64>().unwrap()),
        "records" => do_records(args[2].parse::<u64>().unwrap()),
        _ => {
            println!("Did not understand command: {}", args[1]);
            die_with_usage();
        }
    }
}
