#ovh debian config

#PYTHON=/home/debian/.pyenv/shims/python
#QWEN=/home/debian/qwen-72b.py
#LOG=/home/debian/log-qwen1.txt
#LOG2=/home/debian/log-qwen2.txt

#container config
PYTHON=/usr/bin/python3
QWEN=/scripts/qwen-72b.py
LOG=/logs/log-runner.txt
LOG2=/logs/log-qwen.txt

if [ ! -f $LOG ];then
  touch $LOG
fi

echo "--------" >> $LOG
date >> $LOG
echo "call qwen 1 $1 2 $2 3 $3" >> $LOG
$PYTHON $QWEN "$1" "$2" "$3" > $LOG2 2>&1
echo "qwen has finished" >> $LOG
