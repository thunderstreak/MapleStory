# 编译说明

## 快速编译

```bash
# 1. 下载依赖库（首次编译需要）
./download-deps.sh

# 2. 编译项目
./build.sh
```

编译完成后，新的 jar 文件会生成在 `bin/maple.jar`

## 编译步骤说明

1. **查找所有 Java 源文件**：自动扫描 `src/` 目录下的所有 `.java` 文件

2. **编译 Java 文件**：使用系统 Java 编译器编译所有源文件
   - 编码：UTF-8
   - Java 版本：1.7（保持与原项目一致）
   - 注意：使用 Java 8 编译器编译为 Java 7 字节码是安全的（向后兼容）

3. **复制资源文件**：自动复制以下类型的资源文件到编译输出目录：
   - `.properties` 文件
   - `.xml` 文件
   - 图片文件（`.png`, `.jpg`, `.gif`）
   - `META-INF/` 目录

4. **打包 jar 文件**：使用 `config/MANIFEST.MF` 作为清单文件打包

## Java 版本兼容性说明

### 原项目配置
- **原 JDK 版本**：Java 1.7.0（项目自带的 `jdk/` 目录）
- **编译目标**：Java 7 字节码

### 当前编译配置
- **编译器版本**：系统 Java（Java 8 或更高）
- **编译目标**：Java 7 字节码（`-source 1.7 -target 1.7`）

### 兼容性说明
✅ **安全**：使用 Java 8+ 编译器编译为 Java 7 字节码是**完全安全**的，因为：
- Java 编译器支持向后兼容编译（cross-compilation）
- 生成的字节码可以在 Java 7+ 的 JVM 上运行
- 编译时会检查代码是否符合 Java 7 语法规范

⚠️ **注意事项**：
- 代码中不能使用 Java 8+ 的特性（如 lambda、Stream API 等）
- 如果代码中使用了 Java 8 特性，编译会报错
- 运行时可以使用 Java 7、8 或更高版本的 JVM

## 依赖库说明

根据 `config/MANIFEST.MF`，项目需要以下依赖库：
- `slf4j-api.jar`
- `mysql-connector-java-bin.jar`
- `slf4j-jdk14.jar`
- `mina-core-2.0.9.jar`

**下载依赖库**：
```bash
./download-deps.sh
```

依赖库会自动下载到 `lib/` 目录。

## 常见问题

### 1. 编译错误：找不到类

如果编译时提示找不到某些类，可能是：
- 依赖库未正确配置
- 需要运行 `./download-deps.sh` 下载依赖库

### 2. 运行时错误：ClassNotFoundException

如果运行时提示找不到类：
- 检查 `config/MANIFEST.MF` 中的 Class-Path 配置
- 确保依赖库在正确的位置
- 或者将依赖库打包进 jar 文件（需要修改编译脚本）

### 3. 编码问题

如果编译时出现编码错误：
- 确保所有源文件使用 UTF-8 编码
- 编译脚本已设置 `-encoding UTF-8` 选项

### 4. Java 版本问题

**Q: 使用 Java 8 编译器编译 Java 7 代码会有问题吗？**
A: 不会有问题。Java 编译器支持交叉编译（cross-compilation），可以指定 `-source` 和 `-target` 选项来编译为旧版本的字节码。只要代码中没有使用新版本的特性，就是完全安全的。

**Q: 运行时需要使用哪个 Java 版本？**
A: 由于编译为 Java 7 字节码，运行时至少需要 Java 7 或更高版本的 JVM。Java 7 字节码可以在 Java 7、8、11 等版本上运行。

## 手动编译（如果需要）

如果自动编译脚本有问题，可以手动编译：

```bash
# 1. 创建编译输出目录
mkdir -p build/classes

# 2. 编译所有 Java 文件
javac -encoding UTF-8 -source 1.7 -target 1.7 \
    -d build/classes \
    -cp "$(find lib -name "*.jar" | tr '\n' ':')" \
    $(find src -name "*.java")

# 3. 复制资源文件
find src -type f \( -name "*.properties" -o -name "*.xml" \) \
    -exec sh -c 'mkdir -p build/classes/$(dirname {}) && cp {} build/classes/{}' \;

# 4. 打包 jar
jar cfm bin/maple.jar config/MANIFEST.MF -C build/classes .
```

## 验证编译结果

编译完成后，可以验证 jar 文件：

```bash
# 查看 jar 文件内容
jar tf bin/maple.jar | head -20

# 查看清单文件
jar xf bin/maple.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
```
