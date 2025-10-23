#!/bin/bash -l
#SBATCH -J hyl.lab3.remain2
#SBATCH -A edu25.dd2443
#SBATCH -p shared
#SBATCH --time=01:00:00
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=48
#SBATCH --output=output5.txt
#SBATCH --error=error5.txt

set -e
cd src/
ml java 2>&1 >/dev/null
make all 2>&1 >/dev/null

SETNAME=(Default LocalLog GlobalLog Extra)
OPS=(1:1:8 1:1:0)
MAXVALUE=100000
OPSNUM=1000000
WARMUP=3
MEASUREMENT=5

srun java -cp . Main 48 Extra Normal $MAXVALUE 1:1:8 $OPSNUM $WARMUP $MEASUREMENT

for setn in "${SETNAME[@]}"; do
    srun java -cp . Main 48 $setn Normal $MAXVALUE 1:1:0 $OPSNUM $WARMUP $MEASUREMENT
done