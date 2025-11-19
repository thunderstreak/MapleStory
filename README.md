# MapleStory

> linux冒险岛079版本，支持库也合并成了一个jar包，去除了原版的一些QQ群推广游戏安装

### 步骤

* 第一步

1. 安装mysql数据库，数据库支持5.7版本，5.5.3之前的版本不支持utf8mb4字符集会报错
```shell
ERROR 1115 (42000) at line 213: Unknown character set: 'utf8mb4'

# 一次性解压、替换字符集并导入
gunzip -c /mapleStory/maplestory/ms_20210813_234816.sql.gz | \
sed 's/utf8mb4/utf8/g' | \
sed 's/utf8mb4_unicode_ci/utf8_general_ci/g' | \
sed 's/utf8mb4_general_ci/utf8_general_ci/g' | \
mysql -ugame -p123456 maple
```

2. 创建数据库
```shell
mysql -u root -p password -e "CREATE DATABASE IF NOT EXISTS maple;"
```

3. 导入sql文件
```shell
# .sql 格式
mysql -u root -p password maple < /home/maplestory/server/ms_20210813_234816.sql

# .sql.gz 格式
gunzip < /home/maplestory/server/ms_20210813_234816.sql.gz | mysql -u root -p password maple
```

2. 修改config下面的db.properties 文件中的数据库连接信息
```java
url = jdbc:mysql://ip:3306/maple?autoReconnect=true&characterEncoding=UTF-8
```
如果出现错误,需要添加连接过滤zeroDateTimeBehavior=convertToNull
```java
java.sql.SQLException: Cannot convert value '0000-00-00 00:00:00' from column 24 to TIMESTAMP.
```

3. server.properties 是服务端的配置信息
```text
RoyMS.IP = ip
```

4. 开放端口
```shell
# 启动端口
9595
# 商城端口
8600
# 频道端口
2525
2526
2527
2528
2529
2530
```

* 第二步：运行服务端

```shell
# 启动服务（后台运行，日志输出到 logs/server.log）
./run.sh

# 停止服务
./stop.sh

# 查看日志
tail -f logs/server.log
```

* 第三步
[下载客户端](https://www.aliyundrive.com/s/RzCSPTXc5RA)

```shell
# cmd运行
MapleStory.exe ip 9595

# 或者编辑login.bat文件，修改里面的ip，拷贝到客户端目录中双击运行
taskkill /im MapleStory.exe /f
MapleStory.exe ip 9595
```
