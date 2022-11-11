use std::env;
use std::process;

fn zaremba_tau(n: i64) -> (f64, i32) {
    // Safety margin for floats, because I don't want to think if it's even needed...
    let divisor_limit = (n as f64).sqrt() as i64 + 1;

    let mut sum = 0_f64;
    let mut divisor_count = 0_i32;

    for smaller_divisor in 1..(divisor_limit+1) {
        if n % smaller_divisor != 0 {
            continue // not actually a divisor!
        }

        let larger_divisor = n / smaller_divisor;
        if larger_divisor < smaller_divisor {
            // We've *passed* the geometric midpoint, and should not include
            // either value. Could be redundant with divisorLimit but that one
            // is padded a little for safety. This check is more precise.
            break
        }

        sum += (smaller_divisor as f64).ln() / (smaller_divisor as f64);
        divisor_count += 1;

        if larger_divisor > smaller_divisor {
            sum += (larger_divisor as f64).ln() / (larger_divisor as f64);
            divisor_count += 1;
        } else {
            // We've either reached the sqrt mark exactly (n is a perfect
            // square) and need to stop -- use the small divisor but not the
            // larger one, since that would be overcounting.
            break
        }
    }

    (sum, divisor_count)
}

fn do_single(n: i64) {
    let (z, tau) = zaremba_tau(n);
    let ratio = z / (tau as f64).ln();
    println!(
        "z({n}) = {z}\ttau({n}) = {tau}\tz({n}/ln(tau({n})) = {ratio}",
        n = n, z = z, tau = tau, ratio = ratio
    )
}

fn do_records(max_n: i64) {
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
        "single" => do_single(args[2].parse::<i64>().unwrap()),
        "records" => do_records(args[2].parse::<i64>().unwrap()),
        _ => {
            println!("Did not understand command: {}", args[1]);
            die_with_usage();
        }
    }
}
