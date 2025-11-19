#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 确保 logs 目录存在
mkdir -p logs

# 检查服务是否已经在运行
if pgrep -f "maple.jar" > /dev/null; then
    echo "服务已经在运行中，请先使用 ./stop.sh 停止服务"
    exit 1
fi

# 日志文件路径
LOG_FILE="logs/server.log"

echo "正在启动服务..."
echo "日志文件: $LOG_FILE"
echo "使用 ./stop.sh 可以停止服务"

# 删除旧日志
if [ -f "$LOG_FILE" ]; then
    rm -f "$LOG_FILE"
fi

# 将启动信息输出到日志文件
echo "
+----------------------------------------------------------------------
|                   冒险岛079 FOR CentOS/Ubuntu/Debian
+----------------------------------------------------------------------
" >> "$LOG_FILE"

# 检测 Java 可执行文件
# 优先使用系统 Java，如果系统没有则尝试使用项目自带的 JDK
if command -v java >/dev/null 2>&1; then
    # 使用系统 Java
    JAVA_CMD="java"
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo "使用系统 Java: $JAVA_VERSION" >> "$LOG_FILE"
elif [ -f "$SCRIPT_DIR/jdk/jre/bin/java" ]; then
    # 使用项目自带的 JDK
    JAVA_CMD="$SCRIPT_DIR/jdk/jre/bin/java"
    echo "使用项目自带的 JDK" >> "$LOG_FILE"
else
    echo "错误: 找不到 java，请确保系统已安装 Java 或 jdk 目录存在" >> "$LOG_FILE"
    echo "错误: 找不到 java，请确保系统已安装 Java 或 jdk 目录存在"
    exit 1
fi

# 构建 classpath：包含 jar 文件和 lib 目录下的所有依赖
if [ -d "lib" ] && [ "$(ls -A lib/*.jar 2>/dev/null)" ]; then
    CLASSPATH="./bin/maple.jar:$(find lib -name "*.jar" | tr '\n' ':')"
    # 移除末尾的冒号
    CLASSPATH="${CLASSPATH%:}"
else
    CLASSPATH="./bin/maple.jar"
fi

# 后台运行 Java 服务，并将输出重定向到日志文件
nohup "$JAVA_CMD" -cp "$CLASSPATH" -server -DhomePath=./config/ -DscriptsPath=./scripts/ -DwzPath=./scripts/wz -Xms512m -Xmx2048m -XX:PermSize=256m -XX:MaxPermSize=512m -XX:MaxNewSize=512m server.Start >> "$LOG_FILE" 2>&1 &

# 获取进程ID
PID=$!

# 等待一下，检查进程是否成功启动
sleep 2
if ps -p $PID > /dev/null 2>&1; then
    echo "服务已启动，进程ID: $PID"
    echo "查看日志: tail -f $LOG_FILE"
else
    echo "服务启动失败，请查看日志: $LOG_FILE"
    exit 1
fi

