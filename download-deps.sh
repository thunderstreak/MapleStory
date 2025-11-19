#!/bin/bash

# 依赖库下载脚本
# 根据 config/MANIFEST.MF 中列出的依赖

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

LIB_DIR="lib"
mkdir -p "$LIB_DIR"

echo "=========================================="
echo "    下载项目依赖库"
echo "=========================================="
echo ""

# 依赖库列表（根据 MANIFEST.MF）
# 格式: jar文件名|下载URL
DEPS=(
    "slf4j-api.jar|https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
    "slf4j-jdk14.jar|https://repo1.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.36/slf4j-jdk14-1.7.36.jar"
    "mina-core-2.0.9.jar|https://repo1.maven.org/maven2/org/apache/mina/mina-core/2.0.9/mina-core-2.0.9.jar"
    "mysql-connector-java-bin.jar|https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.49/mysql-connector-java-5.1.49.jar"
)

# 检查是否有 curl 或 wget
if command -v curl >/dev/null 2>&1; then
    DOWNLOAD_CMD="curl -L -o"
elif command -v wget >/dev/null 2>&1; then
    DOWNLOAD_CMD="wget -O"
else
    echo "错误: 需要 curl 或 wget 来下载文件"
    exit 1
fi

# 下载每个依赖
for dep in "${DEPS[@]}"; do
    jar_name="${dep%%|*}"
    url="${dep#*|}"
    file_path="$LIB_DIR/$jar_name"
    
    if [ -f "$file_path" ]; then
        echo "✓ $jar_name 已存在，跳过"
    else
        echo "正在下载 $jar_name..."
        $DOWNLOAD_CMD "$file_path" "$url"
        if [ $? -eq 0 ]; then
            echo "✓ $jar_name 下载完成"
        else
            echo "✗ $jar_name 下载失败"
        fi
    fi
    echo ""
done

echo "=========================================="
echo "依赖库下载完成！"
echo "位置: $LIB_DIR/"
echo "=========================================="

