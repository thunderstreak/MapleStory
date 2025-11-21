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

# 删除 logs/ 目录下的所有日志文件
if [ -d "logs" ]; then
    echo "正在清理 logs/ 目录下的所有日志文件..."
    # 删除 logs/ 目录及其子目录下的所有文件（保留目录结构）
    find logs -type f -delete 2>/dev/null || true
    echo "日志文件清理完成"
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

# 检测 Java 版本，Java 8+ 使用 Metaspace，Java 7 使用 PermGen
JAVA_VERSION_OUTPUT=$("$JAVA_CMD" -version 2>&1 | head -n 1)
# 提取主版本号（支持 "1.8.0" 和 "8" 两种格式）
JAVA_VER=$(echo "$JAVA_VERSION_OUTPUT" | grep -oE 'version "?([0-9]+\.)?[0-9]+' | grep -oE '[0-9]+' | head -1)
# 如果是 "1.8" 格式，提取第二个数字；如果是 "8" 格式，直接使用
if echo "$JAVA_VERSION_OUTPUT" | grep -qE 'version "1\.'; then
    JAVA_MAJOR=$(echo "$JAVA_VERSION_OUTPUT" | grep -oE 'version "1\.([0-9]+)' | grep -oE '[0-9]+$' | head -1)
else
    JAVA_MAJOR=$JAVA_VER
fi

if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -ge 8 ]; then
    # Java 8 及更高版本，使用 Metaspace
    JVM_ARGS="-Xms512m -Xmx2048m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:MaxNewSize=512m"
    echo "检测到 Java 8+ (主版本: $JAVA_MAJOR)，使用 Metaspace 参数" >> "$LOG_FILE"
else
    # Java 7 及更早版本，使用 PermGen
    JVM_ARGS="-Xms512m -Xmx2048m -XX:PermSize=256m -XX:MaxPermSize=512m -XX:MaxNewSize=512m"
    echo "检测到 Java 7 (主版本: $JAVA_MAJOR)，使用 PermGen 参数" >> "$LOG_FILE"
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
nohup "$JAVA_CMD" -cp "$CLASSPATH" -server -DhomePath=./config/ -DscriptsPath=./scripts/ -DwzPath=./scripts/wz $JVM_ARGS server.Start >> "$LOG_FILE" 2>&1 &

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

