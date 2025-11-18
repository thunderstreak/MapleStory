# MapleStory

> 原版是win版的一键搭建，弄成了linux的一键，支持库也合并成了一个jar包，去除了原版的一些QQ群推广游戏安装大的步骤分为三步

### 步骤

* 第一步

1. 安装mysql数据库、导入sql文件
2. 修改一下config下面的db.properties 文件中的数据库连接信息就行了
3. server.properties 是服务端的配置信息，可看着修改
4. 开放端口：9595、8600、2525~2530

* 第二步：运行服务端

```shell
# 直接运行
./start.sh

# 或者后台运行
nohup bash ./start.sh > logs/app.log 2>&1 &

# 查看进程
ps aux | grep start.sh

# 查看日志
tail -f logs/app.log
```

* 第三步：运行游戏 下载个客户端，cmd运行
MapleStory.exe 你的ip 9595
