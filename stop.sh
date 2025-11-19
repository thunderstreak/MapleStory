#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 查找运行中的 maple.jar 进程
PIDS=$(pgrep -f "maple.jar")

if [ -z "$PIDS" ]; then
    echo "服务未运行"
    exit 0
fi

echo "找到运行中的服务进程: $PIDS"
echo "正在停止服务..."

# 尝试优雅停止（发送 SIGTERM 信号）
for PID in $PIDS; do
    echo "停止进程 $PID..."
    kill -TERM $PID 2>/dev/null
done

# 等待进程退出（最多等待10秒）
for i in {1..10}; do
    sleep 1
    REMAINING=$(pgrep -f "maple.jar")
    if [ -z "$REMAINING" ]; then
        echo "服务已成功停止"
        exit 0
    fi
done

# 如果10秒后还有进程在运行，强制杀死
REMAINING=$(pgrep -f "maple.jar")
if [ -n "$REMAINING" ]; then
    echo "优雅停止失败，强制停止进程..."
    for PID in $REMAINING; do
        kill -9 $PID 2>/dev/null
    done
    sleep 1
    
    if pgrep -f "maple.jar" > /dev/null; then
        echo "警告: 部分进程可能未能完全停止"
        exit 1
    else
        echo "服务已强制停止"
    fi
fi

