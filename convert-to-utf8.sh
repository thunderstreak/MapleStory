#!/bin/bash

# 项目编码转换脚本 - 将所有文件转换为 UTF-8 编码

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "    项目文件编码转换为 UTF-8"
echo "=========================================="
echo ""

# 检查是否有 iconv 或 Python
HAS_ICONV=false
HAS_PYTHON=false

if command -v iconv >/dev/null 2>&1; then
    HAS_ICONV=true
    echo "✓ 使用 iconv 进行转换"
elif command -v python3 >/dev/null 2>&1; then
    HAS_PYTHON=true
    echo "✓ 使用 Python3 进行转换"
else
    echo "错误: 需要 iconv 或 python3 来转换编码"
    exit 1
fi

# 统计变量
TOTAL_FILES=0
CONVERTED_FILES=0
SKIPPED_FILES=0
ERROR_FILES=0

# 转换单个文件
convert_file() {
    local file="$1"
    local encoding="$2"
    
    # 跳过二进制文件
    if file "$file" | grep -q "binary"; then
        return 0
    fi
    
    TOTAL_FILES=$((TOTAL_FILES + 1))
    
    # 如果已经是 UTF-8 或 us-ascii，跳过
    # us-ascii 是 UTF-8 的子集，可以认为是 UTF-8
    if [ "$encoding" = "utf-8" ] || [ "$encoding" = "us-ascii" ]; then
        SKIPPED_FILES=$((SKIPPED_FILES + 1))
        return 0
    fi
    
    # 创建临时文件
    local tmp_file="${file}.utf8.tmp"
    
    if [ "$HAS_ICONV" = true ]; then
        # 使用 iconv 转换
        # 尝试多种可能的源编码
        local source_encodings=("$encoding" "gbk" "gb2312" "big5" "iso-8859-1" "latin1")
        
        for src_enc in "${source_encodings[@]}"; do
            if iconv -f "$src_enc" -t UTF-8 "$file" > "$tmp_file" 2>/dev/null; then
                if [ -f "$tmp_file" ] && [ -s "$tmp_file" ]; then
                    mv "$tmp_file" "$file"
                    CONVERTED_FILES=$((CONVERTED_FILES + 1))
                    echo "  ✓ 转换: $file ($src_enc -> UTF-8)"
                    return 0
                fi
            fi
            rm -f "$tmp_file"
        done
    elif [ "$HAS_PYTHON" = true ]; then
        # 使用 Python 转换
        python3 << EOF
import sys
import codecs

try:
    # 尝试检测编码
    with open("$file", 'rb') as f:
        content = f.read()
    
    # 尝试常见编码
    encodings = ['$encoding', 'gbk', 'gb2312', 'big5', 'latin1', 'iso-8859-1']
    decoded = None
    used_encoding = None
    
    for enc in encodings:
        try:
            decoded = content.decode(enc)
            used_encoding = enc
            break
        except (UnicodeDecodeError, LookupError):
            continue
    
    if decoded is None:
        # 如果都失败，尝试忽略错误
        decoded = content.decode('utf-8', errors='ignore')
        used_encoding = 'utf-8 (with errors ignored)'
    
    # 写入 UTF-8
    with open("$tmp_file", 'w', encoding='utf-8') as f:
        f.write(decoded)
    
    sys.exit(0)
except Exception as e:
    print(f"Error converting $file: {e}", file=sys.stderr)
    sys.exit(1)
EOF
        if [ $? -eq 0 ] && [ -f "$tmp_file" ]; then
            mv "$tmp_file" "$file"
            CONVERTED_FILES=$((CONVERTED_FILES + 1))
            echo "  ✓ 转换: $file -> UTF-8"
            return 0
        fi
    fi
    
    ERROR_FILES=$((ERROR_FILES + 1))
    echo "  ✗ 转换失败: $file"
    rm -f "$tmp_file"
    return 1
}

# 检测文件编码（简化版）
detect_encoding() {
    local file="$1"
    local detected=$(file -I "$file" 2>/dev/null | cut -d= -f2 | tr -d ';' | tr -d ' ')
    
    if [ -z "$detected" ] || [ "$detected" = "binary" ]; then
        detected="unknown"
    fi
    
    # 处理特殊编码名称
    case "$detected" in
        "iso-8859-1"|"latin1")
            detected="iso-8859-1"
            ;;
        "unknown-8bit")
            # 尝试检测实际编码
            detected="gbk"
            ;;
    esac
    
    echo "$detected"
}

# 转换 Java 文件
echo "正在转换 Java 源文件..."
echo "----------------------------------------"
JAVA_COUNT=0
while IFS= read -r -d '' file; do
    encoding=$(detect_encoding "$file")
    convert_file "$file" "$encoding"
    JAVA_COUNT=$((JAVA_COUNT + 1))
done < <(find src -name "*.java" -type f -print0 2>/dev/null)
echo "Java 文件处理完成: $JAVA_COUNT 个文件"
echo ""

# 转换 JS 脚本文件
echo "正在转换 JavaScript 脚本文件..."
echo "----------------------------------------"
JS_COUNT=0
while IFS= read -r -d '' file; do
    encoding=$(detect_encoding "$file")
    convert_file "$file" "$encoding"
    JS_COUNT=$((JS_COUNT + 1))
done < <(find scripts -name "*.js" -type f -print0 2>/dev/null)
echo "JS 文件处理完成: $JS_COUNT 个文件"
echo ""

# 转换配置文件
echo "正在转换配置文件..."
echo "----------------------------------------"
PROP_COUNT=0
while IFS= read -r -d '' file; do
    encoding=$(detect_encoding "$file")
    convert_file "$file" "$encoding"
    PROP_COUNT=$((PROP_COUNT + 1))
done < <(find config src -name "*.properties" -type f -print0 2>/dev/null)
echo "配置文件处理完成: $PROP_COUNT 个文件"
echo ""

# 转换 XML 文件
echo "正在转换 XML 文件..."
echo "----------------------------------------"
XML_COUNT=0
while IFS= read -r -d '' file; do
    encoding=$(detect_encoding "$file")
    convert_file "$file" "$encoding"
    XML_COUNT=$((XML_COUNT + 1))
done < <(find scripts/wz -name "*.xml" -type f -print0 2>/dev/null)
echo "XML 文件处理完成: $XML_COUNT 个文件"
echo ""

# 输出统计信息
echo "=========================================="
echo "转换完成！"
echo "=========================================="
echo "总文件数: $TOTAL_FILES"
echo "已转换: $CONVERTED_FILES"
echo "已跳过 (已是 UTF-8): $SKIPPED_FILES"
echo "转换失败: $ERROR_FILES"
echo "=========================================="

if [ $ERROR_FILES -gt 0 ]; then
    echo ""
    echo "警告: 有 $ERROR_FILES 个文件转换失败，请手动检查"
    exit 1
fi

