echo "
+----------------------------------------------------------------------
|                   冒险岛079 FOR CentOS/Ubuntu/Debian
+----------------------------------------------------------------------
"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 检测 Java 可执行文件
# 优先使用系统 Java，如果系统没有则尝试使用项目自带的 JDK
if command -v java >/dev/null 2>&1; then
    # 使用系统 Java
    JAVA_CMD="java"
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo "使用系统 Java: $JAVA_VERSION"
elif [ -f "$SCRIPT_DIR/jdk/jre/bin/java" ]; then
    # 使用项目自带的 JDK
    JAVA_CMD="$SCRIPT_DIR/jdk/jre/bin/java"
    echo "使用项目自带的 JDK"
else
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

"$JAVA_CMD" -cp "$CLASSPATH" -server -DhomePath=./config/ -DscriptsPath=./scripts/ -DwzPath=./scripts/wz -Xms512m -Xmx2048m -XX:PermSize=256m -XX:MaxPermSize=512m -XX:MaxNewSize=512m server.Start
