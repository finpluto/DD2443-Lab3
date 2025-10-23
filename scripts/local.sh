## local testing script
## normal and uniform distribute
set -e

THREADS=(1 2 4 8)
DISTRIBUTION=(Uniform Normal)
OPS=(1:1:8 1:1:0)
MAXVALUE=10000
OPSNUM=100000
WARMUP=3
MEASUREMENT=5


echo -e "SetName,Distribution,ThreadNumber,OPS,Time,Discrepancy"
for tnum in "${THREADS[@]}"; do
    for dis in "${DISTRIBUTION[@]}"; do
        for ops in "${OPS[@]}"; do
            java -cp ../src/ Main $tnum Default $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
            java -cp ../src/ Main $tnum Locked $dis $MAXVALUE "$ops" $OPSNUM $WARMUP $MEASUREMENT
        done
    done
done