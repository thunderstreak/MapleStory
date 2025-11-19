#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "    冒险岛079 项目编译脚本"
echo "=========================================="

# 设置 JDK 路径
# 优先使用系统 Java，如果系统没有则尝试使用项目自带的 JDK
if command -v javac >/dev/null 2>&1; then
    # 使用系统 Java
    JAVAC="javac"
    JAR="jar"
    JAVA_VERSION=$(javac -version 2>&1 | head -1)
    echo "使用系统 Java: $JAVA_VERSION"
elif [ -f "$SCRIPT_DIR/jdk/bin/javac" ]; then
    # 使用项目自带的 JDK
    JAVA_HOME="$SCRIPT_DIR/jdk"
    JAVAC="$JAVA_HOME/bin/javac"
    JAR="$JAVA_HOME/bin/jar"
    echo "使用项目自带的 JDK"
else
    echo "错误: 找不到 javac，请确保系统已安装 Java 或 jdk 目录存在"
    exit 1
fi

# 创建编译输出目录
BUILD_DIR="build"
CLASSES_DIR="$BUILD_DIR/classes"
echo "创建编译目录: $CLASSES_DIR"
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

# 源文件目录
SRC_DIR="src"

# 查找所有 Java 文件
echo "正在查找所有 Java 源文件..."
JAVA_FILES=$(find "$SRC_DIR" -name "*.java" | tr '\n' ' ')

if [ -z "$JAVA_FILES" ]; then
    echo "错误: 没有找到 Java 源文件"
    exit 1
fi

JAVA_COUNT=$(echo "$JAVA_FILES" | wc -w)
echo "找到 $JAVA_COUNT 个 Java 文件"

# 编译选项
# -encoding UTF-8: 使用 UTF-8 编码
# -source 1.7: 使用 Java 7 语法（保持与原项目一致）
# -target 1.7: 编译为 Java 7 字节码（保持与原项目一致）
# -d: 输出目录
# -cp: 类路径（如果需要依赖库，在这里添加）
# 注意：使用 Java 8 编译器编译为 Java 7 字节码是安全的（向后兼容）
COMPILE_OPTS="-encoding UTF-8 -source 1.7 -target 1.7 -d $CLASSES_DIR"

# 检查是否有 lib 目录（依赖库）
# 注意：如果依赖库已经打包在 jar 中，则不需要这里添加
if [ -d "lib" ] && [ "$(ls -A lib/*.jar 2>/dev/null)" ]; then
    # 将所有 jar 文件添加到类路径（macOS 不支持通配符，需要明确列出）
    CLASSPATH=$(find lib -name "*.jar" | tr '\n' ':')
    # 移除末尾的冒号
    CLASSPATH="${CLASSPATH%:}"
    COMPILE_OPTS="$COMPILE_OPTS -cp $CLASSPATH"
    echo "发现 lib 目录，添加依赖库到类路径"
    echo "依赖库: $(ls lib/*.jar | tr '\n' ' ')"
else
    echo "未发现 lib 目录，假设依赖库已打包在 jar 中或使用系统类路径"
fi

# 编译所有 Java 文件
echo ""
echo "开始编译..."
echo "=========================================="
$JAVAC $COMPILE_OPTS $JAVA_FILES 2>&1 | tee "$BUILD_DIR/compile.log"

# 检查编译结果
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo ""
    echo "=========================================="
    echo "编译失败！请查看 $BUILD_DIR/compile.log 了解详情"
    echo "=========================================="
    exit 1
fi

echo ""
echo "编译成功！"

# 复制资源文件（properties、xml 等）
echo "复制资源文件..."
find "$SRC_DIR" -type f \( -name "*.properties" -o -name "*.xml" -o -name "*.png" -o -name "*.jpg" -o -name "*.gif" \) | while read file; do
    # 获取相对路径
    rel_path=${file#$SRC_DIR/}
    target_file="$CLASSES_DIR/$rel_path"
    target_dir=$(dirname "$target_file")
    mkdir -p "$target_dir"
    cp "$file" "$target_file"
done

# 复制 META-INF 目录
if [ -d "$SRC_DIR/META-INF" ]; then
    echo "复制 META-INF 目录..."
    cp -r "$SRC_DIR/META-INF" "$CLASSES_DIR/"
fi

# 创建 jar 文件
echo ""
echo "打包 jar 文件..."
JAR_FILE="bin/maple.jar"
mkdir -p bin

# 使用 MANIFEST.MF（如果存在）
MANIFEST_FILE="config/MANIFEST.MF"
if [ -f "$MANIFEST_FILE" ]; then
    echo "使用 MANIFEST 文件: $MANIFEST_FILE"
    $JAR cfm "$JAR_FILE" "$MANIFEST_FILE" -C "$CLASSES_DIR" .
else
    echo "使用默认 MANIFEST"
    $JAR cf "$JAR_FILE" -C "$CLASSES_DIR" .
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "编译完成！"
    echo "JAR 文件: $JAR_FILE"
    echo "文件大小: $(du -h "$JAR_FILE" | cut -f1)"
    echo "=========================================="
    echo ""
    echo "可以使用以下命令运行:"
    echo "  ./run.sh    # 后台运行"
    echo "  ./start.sh  # 前台运行"
else
    echo ""
    echo "=========================================="
    echo "打包失败！"
    echo "=========================================="
    exit 1
fi

