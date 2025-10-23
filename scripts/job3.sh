#!/bin/bash -l
#SBATCH -J hyl.lab3.remain2
#SBATCH -A edu25.dd2443
#SBATCH -p shared
#SBATCH --time=01:00:00
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=48
#SBATCH --output=output3.txt
#SBATCH --error=error3.txt

set -e
cd src/
ml java 2>&1 >/dev/null
make all 2>&1 >/dev/null

SETNAME=(Default LocalLog GlobalLog Extra)
THREADS=(1 2 4 8 16 32)
DISTRIBUTION=(Uniform Normal)
OPS=(1:1:8 1:1:0)
MAXVALUE=100000
OPSNUM=1000000
WARMUP=3
MEASUREMENT=5

## retest LocalLog again
for tnum in "${THREADS[@]}"; do
    for dis in "${DISTRIBUTION[@]}"; do
        for ops in "${OPS[@]}"; do
            srun java -cp . Main $tnum LocalLog $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
        done
    done
done

## test last
srun java -cp . Main 32 Extra Normal $MAXVALUE 1:1:0 $OPSNUM $WARMUP $MEASUREMENT

for dis in "${DISTRIBUTION[@]}"; do
    for ops in "${OPS[@]}"; do
        for setn in "${SETNAME[@]}"; do
            srun java -cp . Main 48 $setn $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
        done
    done
done