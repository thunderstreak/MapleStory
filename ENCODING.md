# 项目编码说明

## 当前编码状态

项目已统一使用 **UTF-8** 编码。

### 编码分布

- **Java 源文件** (`src/*.java`): 全部为 UTF-8 或 US-ASCII（UTF-8 子集）
- **JavaScript 脚本** (`scripts/**/*.js`): 全部为 UTF-8 或 US-ASCII
- **配置文件** (`config/*.properties`, `src/*.properties`): 全部为 UTF-8

### 编码说明

1. **US-ASCII 是 UTF-8 的子集**
   - 只包含 ASCII 字符（0-127）的文件可能被识别为 `us-ascii`
   - 这些文件实际上是有效的 UTF-8 文件
   - 不需要额外转换

2. **UTF-8 编码的优势**
   - 支持所有 Unicode 字符（包括中文、日文、韩文等）
   - 向后兼容 ASCII
   - 是 Java 和现代 Web 开发的标准编码

## 编码转换工具

项目提供了两个编码转换脚本：

### 1. `convert-to-utf8.py`
- 智能检测文件编码
- 自动转换为 UTF-8
- 支持多种源编码（GBK、GB2312、Big5、ISO-8859-1 等）

### 2. `ensure-utf8.sh`
- 确保所有文件都是 UTF-8
- 验证文件编码正确性
- 修复编码错误

## 使用方法

```bash
# 方法 1: 使用 Python 脚本（推荐）
python3 convert-to-utf8.py

# 方法 2: 使用 Shell 脚本
./ensure-utf8.sh
```

## 编译时的编码设置

编译脚本 `build.sh` 已配置为使用 UTF-8：

```bash
COMPILE_OPTS="-encoding UTF-8 -source 1.7 -target 1.7 -d $CLASSES_DIR"
```

## 运行时编码

### Java 文件
- 编译时使用 `-encoding UTF-8` 选项
- 确保源文件以 UTF-8 编码保存

### JavaScript 脚本
- 脚本加载时使用 `EncodingDetect.getJavaEncode()` 自动检测编码
- 支持多种编码格式的脚本文件
- 建议统一使用 UTF-8 以避免编码问题

## 常见问题

### Q: 为什么有些文件显示为 us-ascii？
A: US-ASCII 是 UTF-8 的子集。如果文件只包含 ASCII 字符（0-127），可能被识别为 us-ascii，但实际上是有效的 UTF-8 文件。

### Q: 转换后中文显示乱码怎么办？
A: 
1. 确认源文件的实际编码
2. 使用正确的源编码重新转换
3. 检查文件是否在转换过程中损坏

### Q: 如何验证文件编码？
A: 使用以下命令：
```bash
# 检查单个文件
file -I 文件名

# 检查目录下所有文件
find . -name "*.java" -exec file -I {} \;
```

## 注意事项

1. **备份重要文件**：转换前建议备份重要文件
2. **版本控制**：转换后提交到版本控制系统时，确保 Git 配置为 UTF-8
3. **IDE 设置**：确保 IDE（如 IntelliJ IDEA）的文件编码设置为 UTF-8

## Git 配置（可选）

如果使用 Git，建议配置：

```bash
# 设置 Git 使用 UTF-8
git config --global core.quotepath false
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8
```

