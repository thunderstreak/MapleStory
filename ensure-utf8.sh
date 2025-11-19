#!/bin/bash

# 确保所有文件都是 UTF-8 编码
# 即使文件是 us-ascii（UTF-8 子集），也统一转换为 UTF-8

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "    确保所有文件为 UTF-8 编码"
echo "=========================================="
echo ""

# 使用 Python 进行转换（更可靠）
python3 << 'PYTHON_SCRIPT'
import os
import sys
from pathlib import Path

def ensure_utf8(file_path):
    """确保文件是 UTF-8 编码"""
    try:
        # 尝试以 UTF-8 读取
        try:
            with open(file_path, 'r', encoding='utf-8', errors='strict') as f:
                content = f.read()
            # 如果能成功读取，文件已经是 UTF-8
            return True, "已是 UTF-8"
        except UnicodeDecodeError:
            # 不是 UTF-8，需要转换
            pass
        
        # 尝试常见编码读取
        encodings = ['gbk', 'gb2312', 'big5', 'iso-8859-1', 'latin1', 'cp1252']
        content = None
        used_encoding = None
        
        for enc in encodings:
            try:
                with open(file_path, 'r', encoding=enc, errors='replace') as f:
                    content = f.read()
                used_encoding = enc
                break
            except:
                continue
        
        if content is None:
            # 最后尝试：使用 UTF-8 并忽略错误
            with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
                content = f.read()
            used_encoding = 'utf-8 (修复)'
        
        # 写入 UTF-8
        with open(file_path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        
        if used_encoding == 'utf-8 (修复)':
            return True, "修复为 UTF-8"
        else:
            return True, f"{used_encoding} -> UTF-8"
    except Exception as e:
        return False, str(e)

def process_directory(directory, extensions, exclude_dirs=None):
    """处理目录下的文件"""
    if exclude_dirs is None:
        exclude_dirs = {'.git', 'build', 'bin', 'jdk', 'logs', '__pycache__', '.idea'}
    
    stats = {'total': 0, 'converted': 0, 'skipped': 0, 'errors': 0}
    directory = Path(directory)
    
    for ext in extensions:
        for file_path in directory.rglob(f"**/*{ext}"):
            # 跳过排除的目录
            if any(excluded in file_path.parts for excluded in exclude_dirs):
                continue
            
            # 跳过二进制文件
            if file_path.suffix.lower() in ['.class', '.jar', '.zip', '.png', '.jpg', '.gif']:
                continue
            
            stats['total'] += 1
            
            try:
                success, message = ensure_utf8(file_path)
                if success:
                    if message == "已是 UTF-8":
                        stats['skipped'] += 1
                    else:
                        stats['converted'] += 1
                        if stats['converted'] <= 50 or stats['converted'] % 100 == 0:
                            print(f"  ✓ {file_path.relative_to(directory)} ({message})")
                else:
                    stats['errors'] += 1
                    print(f"  ✗ {file_path.relative_to(directory)} ({message})", file=sys.stderr)
            except Exception as e:
                stats['errors'] += 1
                print(f"  ✗ {file_path.relative_to(directory)} (错误: {e})", file=sys.stderr)
            
            if stats['total'] % 500 == 0:
                print(f"  进度: 已处理 {stats['total']} 个文件...")
    
    return stats

# 主程序
script_dir = Path(__file__).parent if '__file__' in globals() else Path.cwd()
os.chdir(script_dir)

all_stats = {'total': 0, 'converted': 0, 'skipped': 0, 'errors': 0}

# 处理 Java 文件
print("正在处理 Java 源文件...")
print("-" * 50)
stats = process_directory('src', ['.java'])
for key in all_stats:
    all_stats[key] += stats[key]
print(f"Java 文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
print()

# 处理 JS 文件
print("正在处理 JavaScript 脚本文件...")
print("-" * 50)
stats = process_directory('scripts', ['.js'])
for key in all_stats:
    all_stats[key] += stats[key]
print(f"JS 文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
print()

# 处理配置文件
print("正在处理配置文件...")
print("-" * 50)
stats = process_directory('.', ['.properties'], exclude_dirs={'.git', 'build', 'bin', 'jdk', 'logs', '__pycache__', '.idea', 'scripts'})
for key in all_stats:
    all_stats[key] += stats[key]
print(f"配置文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
print()

# 输出统计
print("=" * 50)
print("处理完成！")
print("=" * 50)
print(f"总文件数: {all_stats['total']}")
print(f"已转换: {all_stats['converted']}")
print(f"已跳过 (已是 UTF-8): {all_stats['skipped']}")
print(f"转换失败: {all_stats['errors']}")
print("=" * 50)

if all_stats['errors'] > 0:
    sys.exit(1)
PYTHON_SCRIPT

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✓ 所有文件已确保为 UTF-8 编码"
else
    echo ""
    echo "✗ 转换过程中出现错误"
    exit $EXIT_CODE
fi

