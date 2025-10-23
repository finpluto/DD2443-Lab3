#!/bin/bash -l
#SBATCH -J hyl.lab3.remain
#SBATCH -A edu25.dd2443
#SBATCH -p shared
#SBATCH --time=00:45:00
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=48
#SBATCH --output=output2.txt
#SBATCH --error=error2.txt

set -e
cd src/
ml java 2>&1 >/dev/null
make all 2>&1 >/dev/null

SETNAME=(Default LocalLog GlobalLog Extra)
THREADS=(1 2 4 8 16)
REMAINTHREADS=(32 48)
DISTRIBUTION=(Uniform Normal)
OPS=(1:1:8 1:1:0)
MAXVALUE=100000
OPSNUM=1000000
WARMUP=3
MEASUREMENT=5

## retest LocalLog
for tnum in "${THREADS[@]}"; do
    for dis in "${DISTRIBUTION[@]}"; do
        for ops in "${OPS[@]}"; do
            srun java -cp . Main $tnum LocalLog $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
        done
    done
done

for tnum in "${REMAINTHREADS[@]}"; do
    for dis in "${DISTRIBUTION[@]}"; do
        for ops in "${OPS[@]}"; do
            for setn in "${SETNAME[@]}"; do
                srun java -cp . Main $tnum $setn $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
            done
        done
    done
done