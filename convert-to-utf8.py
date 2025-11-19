#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
项目文件编码转换为 UTF-8
自动检测文件编码并转换为 UTF-8
"""

import os
import sys
from pathlib import Path

def detect_encoding(file_path):
    """检测文件编码 - 尝试常见编码"""
    # 常见编码列表（按优先级排序）
    encodings_to_try = [
        'utf-8',
        'gbk',
        'gb2312',
        'big5',
        'iso-8859-1',
        'latin1',
        'cp1252',
        'utf-16',
        'utf-16le',
        'utf-16be'
    ]
    
    try:
        for enc in encodings_to_try:
            try:
                with open(file_path, 'r', encoding=enc, errors='strict') as f:
                    content = f.read()
                # 验证可以编码为 UTF-8
                content.encode('utf-8')
                return enc
            except (UnicodeDecodeError, UnicodeError):
                continue
            except LookupError:
                # 编码不存在，跳过
                continue
        
        # 如果都失败，使用 UTF-8 并忽略错误
        return 'utf-8-ignore'
    except Exception as e:
        print(f"检测编码时出错 {file_path}: {e}", file=sys.stderr)
        return None

def convert_to_utf8(file_path):
    """将文件转换为 UTF-8"""
    try:
        # 检测编码
        source_encoding = detect_encoding(file_path)
        if source_encoding is None:
            return False, "无法检测编码"
        
        # 如果已经是 UTF-8，跳过
        if source_encoding.lower() in ['utf-8', 'utf-8-ignore']:
            # 验证文件确实是 UTF-8
            try:
                with open(file_path, 'r', encoding='utf-8', errors='strict') as f:
                    f.read()
                return True, "已是 UTF-8"
            except UnicodeDecodeError:
                # 文件不是有效的 UTF-8，需要转换
                pass
        
        # 读取文件
        if source_encoding == 'utf-8-ignore':
            # 使用错误处理模式
            with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
                content = f.read()
            source_encoding = 'utf-8 (修复)'
        else:
            with open(file_path, 'r', encoding=source_encoding, errors='replace') as f:
                content = f.read()
        
        # 写入 UTF-8
        with open(file_path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        
        if source_encoding == 'utf-8 (修复)':
            return True, "修复为 UTF-8"
        else:
            return True, f"{source_encoding} -> UTF-8"
    except Exception as e:
        return False, str(e)

def process_files(directory, extensions, exclude_dirs=None):
    """处理指定目录下的文件"""
    if exclude_dirs is None:
        exclude_dirs = {'.git', 'build', 'bin', 'jdk', 'logs', '__pycache__', '.idea'}
    
    stats = {
        'total': 0,
        'converted': 0,
        'skipped': 0,
        'errors': 0
    }
    
    directory = Path(directory)
    
    for ext in extensions:
        pattern = f"**/*{ext}"
        for file_path in directory.rglob(pattern):
            # 跳过排除的目录
            if any(excluded in file_path.parts for excluded in exclude_dirs):
                continue
            
            # 跳过二进制文件
            if file_path.suffix.lower() in ['.class', '.jar', '.zip', '.png', '.jpg', '.gif']:
                continue
            
            stats['total'] += 1
            
            try:
                success, message = convert_to_utf8(file_path)
                if success:
                    if message == "已是 UTF-8":
                        stats['skipped'] += 1
                    else:
                        stats['converted'] += 1
                        # 只显示转换的文件，跳过的不显示
                        if stats['converted'] % 50 == 0 or stats['converted'] <= 20:
                            print(f"  ✓ {file_path.relative_to(directory)} ({message})")
                else:
                    stats['errors'] += 1
                    print(f"  ✗ {file_path.relative_to(directory)} ({message})", file=sys.stderr)
            except Exception as e:
                stats['errors'] += 1
                print(f"  ✗ {file_path.relative_to(directory)} (错误: {e})", file=sys.stderr)
            
            # 每处理 100 个文件显示一次进度
            if stats['total'] % 100 == 0:
                print(f"  进度: 已处理 {stats['total']} 个文件...")
    
    return stats

def main():
    script_dir = Path(__file__).parent
    os.chdir(script_dir)
    
    print("=" * 50)
    print("    项目文件编码转换为 UTF-8")
    print("=" * 50)
    print()
    
    all_stats = {
        'total': 0,
        'converted': 0,
        'skipped': 0,
        'errors': 0
    }
    
    # 处理 Java 文件
    print("正在转换 Java 源文件...")
    print("-" * 50)
    stats = process_files('src', ['.java'])
    all_stats['total'] += stats['total']
    all_stats['converted'] += stats['converted']
    all_stats['skipped'] += stats['skipped']
    all_stats['errors'] += stats['errors']
    print(f"Java 文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
    print()
    
    # 处理 JS 文件
    print("正在转换 JavaScript 脚本文件...")
    print("-" * 50)
    stats = process_files('scripts', ['.js'])
    all_stats['total'] += stats['total']
    all_stats['converted'] += stats['converted']
    all_stats['skipped'] += stats['skipped']
    all_stats['errors'] += stats['errors']
    print(f"JS 文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
    print()
    
    # 处理配置文件
    print("正在转换配置文件...")
    print("-" * 50)
    stats = process_files('.', ['.properties'], exclude_dirs={'.git', 'build', 'bin', 'jdk', 'logs', '__pycache__', '.idea', 'scripts'})
    all_stats['total'] += stats['total']
    all_stats['converted'] += stats['converted']
    all_stats['skipped'] += stats['skipped']
    all_stats['errors'] += stats['errors']
    print(f"配置文件: 总计 {stats['total']}, 转换 {stats['converted']}, 跳过 {stats['skipped']}, 错误 {stats['errors']}")
    print()
    
    # 输出统计
    print("=" * 50)
    print("转换完成！")
    print("=" * 50)
    print(f"总文件数: {all_stats['total']}")
    print(f"已转换: {all_stats['converted']}")
    print(f"已跳过 (已是 UTF-8): {all_stats['skipped']}")
    print(f"转换失败: {all_stats['errors']}")
    print("=" * 50)
    
    if all_stats['errors'] > 0:
        print()
        print(f"警告: 有 {all_stats['errors']} 个文件转换失败，请手动检查")
        sys.exit(1)

if __name__ == '__main__':
    main()

