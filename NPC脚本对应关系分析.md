# NPC 脚本对应关系分析

## 一、NPC 脚本统计

### 1. 脚本文件数量统计

- **总脚本文件数**：**1631 个**
- **基础 NPC ID 脚本数**：**987 个**（纯数字 ID，不含下划线）
- **带参数的脚本数**：**644 个**（格式：`{npcId}_{参数}.js`）
- **有参数变体的基础 NPC 数**：**50 个**（这些 NPC 有多个脚本变体）

### 2. 脚本文件命名规则

#### 基础格式

- **格式**：`{npcId}.js`
- **示例**：`1002000.js`、`9900004.js`、`1063002.js`
- **说明**：直接使用 NPC ID 作为文件名

#### 带参数格式

- **格式**：`{npcId}_{wh}.js`
- **示例**：`1002000_1.js`、`9900004_9999.js`、`1022003_616.js`
- **说明**：`wh` 参数用于区分同一 NPC 的不同功能脚本

### 3. NPC ID 范围

- **最小 NPC ID**：`1`
- **最大 NPC ID**：`99000042`（特殊格式）
- **常见 NPC ID 范围**：
  - 普通 NPC：`1002000` - `9330085`
  - 系统 NPC：`9900000` - `9901504`
  - 特殊 NPC：`93100700`、`93100831`、`93300833`、`99000042`

---

## 二、NPC 脚本加载机制

### 1. 脚本加载位置

**核心类**：`src/scripting/NPCScriptManager.java`

**关键方法**：

```42:44:src/scripting/NPCScriptManager.java
iv = this.getInvocable("npc" + File.separator + npc + ".js", c, true);
```

### 2. 脚本加载流程

1. **玩家点击 NPC** → 触发 `NPCScriptManager.start(c, npc, wh)`
2. **构建脚本路径**：
   - 基础脚本：`scripts/npc/{npcId}.js`
   - 带参数脚本：`scripts/npc/{npcId}_{wh}.js`
3. **加载脚本文件**：
   - 如果脚本存在 → 执行脚本的 `start()` 函数
   - 如果脚本不存在 → 显示默认消息："欢迎来到#b 冒险岛#k。你找我有什么事吗？\r\n 我的 ID 是: #r{npcId}#k.\r\n 有问题联系#bGM#k"
4. **脚本执行**：
   - 调用 `iv.invokeFunction("start")` 或 `iv.invokeFunction("action", 1, 0, 0)`

### 3. 脚本文件路径

**实际路径**：`scripts/scripts/npc/{npcId}.js`

**系统属性**：通过 `System.getProperty("scripts_path")` 获取脚本根目录

---

## 三、NPC 数据来源

### 1. NPC 名称数据

**数据源**：数据库表 `wz_npcnamedata`

**加载位置**：`src/server/life/MapleLifeFactory.java`

```79:88:src/server/life/MapleLifeFactory.java
try (final PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_npcnamedata ORDER BY `npc`");
        final ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
        MapleLifeFactory.npcNames.put(rs.getInt("npc"), rs.getString("name"));
    }
} catch (SQLException ex) {
    System.out.println("Failed to load npc name data. " + ex);
    FileoutputUtil.outputFileError("logs/数据库异常.txt", ex);
}
System.out.println("共加载NPC：" + MapleLifeFactory.npcNames.size());
```

**说明**：服务器启动时会从数据库加载所有 NPC 的名称，并输出加载数量。

### 2. NPC 位置数据

**数据源**：WZ 文件 `Etc.wz/NpcLocation.img`

**加载位置**：`src/server/life/MapleLifeFactory.java`

```48:55:src/server/life/MapleLifeFactory.java
public static int getNPCLocation(final int npcid) {
    if (MapleLifeFactory.NPCLoc.containsKey(npcid)) {
        return MapleLifeFactory.NPCLoc.get(npcid);
    }
    final int map = MapleDataTool.getIntConvert(Integer.toString(npcid) + "/0", MapleLifeFactory.npclocData, -1);
    MapleLifeFactory.NPCLoc.put(npcid, map);
    return map;
}
```

### 3. NPC 基本信息

**数据源**：WZ 文件 `String.wz/Npc.img`

**加载位置**：`src/server/life/MapleLifeFactory.java`

```244:254:src/server/life/MapleLifeFactory.java
public static MapleNPC getNPC(final int nid) {
    String name = MapleLifeFactory.npcNames.get(nid);
    if (name == null) {
        name = MapleDataTool.getString(nid + "/name", MapleLifeFactory.npcStringData, "MISSINGNO");
        MapleLifeFactory.npcNames.put(nid, name);
    }
    if (name.indexOf("Maple TV") != -1) {
        return null;
    }
    return new MapleNPC(nid, name);
}
```

---

## 四、NPC 与脚本的对应关系

### 1. 一对一关系（基础脚本）

**说明**：大多数 NPC 只有一个基础脚本文件。

**示例**：

- NPC ID `1002000` → 脚本文件 `1002000.js`
- NPC ID `1063002` → 脚本文件 `1063002.js`

### 2. 一对多关系（带参数脚本）

**说明**：部分 NPC 有多个脚本变体，通过参数 `wh` 区分。

**示例**：

- NPC ID `9900004` 有多个脚本：
  - `9900004.js`（基础脚本）
  - `9900004_1.js`
  - `9900004_9999.js`
  - `9900004_121.js`
  - `9900004_212.js`
  - 等等...

**调用方式**：

```java
// 基础脚本
NPCScriptManager.getInstance().start(c, 9900004);

// 带参数脚本
NPCScriptManager.getInstance().start(c, 9900004, 9999);
```

### 3. 无脚本的 NPC

**说明**：如果 NPC 没有对应的脚本文件，系统会显示默认消息。

**处理逻辑**：

```54:70:src/scripting/NPCScriptManager.java
if (iv == null || getInstance() == null) {
    if (wh == 0) {
        switch (GameConstants.game) {
            case 0: {
                cm.sendOk("欢迎来到#b冒险岛#k。你找我有什么事吗？\r\n我的ID是: #r" + npc + "#k.\r\n 有问题联系#bGM#k");
                break;
            }
            default: {
                cm.sendOk("欢迎来到#b冒险岛#k。你找我有什么事吗？\r\n我的ID是: #r" + npc + "#k.\r\n 有问题联系#bGM#k");
                break;
            }
        }
    } else {
        cm.sendOk("欢迎来到#b冒险岛#k。你找我有什么事吗？\r\n我的ID是: #r" + npc + "#k.\r\n 有问题联系#bGM#k");
    }
    cm.dispose();
    return;
}
```

---

## 五、如何确定 NPC 数量

### 1. 从数据库确定

**方法**：查询数据库表 `wz_npcnamedata`

```sql
SELECT COUNT(*) FROM wz_npcnamedata;
```

**说明**：这是最准确的方法，因为所有 NPC 的名称都存储在这里。

### 2. 从脚本文件确定

**方法**：统计脚本文件数量

```bash
# 统计所有脚本文件（包括带参数的）
find scripts/scripts/npc -name "*.js" | wc -l

# 统计基础 NPC ID 数量（不含参数）
find scripts/scripts/npc -name "*.js" | sed 's|.*/||' | sed 's|\.js$||' | grep -E '^[0-9]+$' | wc -l
```

**说明**：脚本文件数量可能少于实际 NPC 数量，因为：

- 不是所有 NPC 都有脚本
- 有些 NPC 可能只有数据库定义，没有脚本文件

### 3. 从 WZ 文件确定

**方法**：解析 WZ 文件 `String.wz/Npc.img` 或 `Etc.wz/NpcLocation.img`

**说明**：这是官方数据源，包含所有 NPC 的定义。

---

## 六、脚本对应关系检查

### 1. 检查脚本是否存在

**方法**：查看 `NPCScriptManager.java` 的日志输出

**日志位置**：

- 脚本错误：`logs/ScriptEx_Log.log`
- 如果脚本不存在，会显示默认消息

### 2. 检查 NPC 是否有脚本

**方法**：在游戏中点击 NPC，如果显示默认消息，说明没有脚本或脚本加载失败。

### 3. 检查脚本与 NPC 的对应关系

**方法**：

1. 查看脚本文件命名是否符合规则
2. 检查脚本中是否使用了正确的 NPC ID
3. 查看服务器日志确认脚本加载情况

---

## 七、总结

### 关键点

1. **NPC 总数**：需要从数据库 `wz_npcnamedata` 表查询，服务器启动时会输出加载数量
2. **脚本总数**：**1631 个**脚本文件
3. **基础 NPC 脚本数**：**987 个**
4. **对应关系**：
   - **一对一**：大多数 NPC 只有一个基础脚本
   - **一对多**：部分 NPC 有多个脚本变体（通过参数区分）
   - **无脚本**：没有脚本的 NPC 会显示默认消息

### 建议

1. **确定 NPC 总数**：查询数据库表 `wz_npcnamedata` 获取准确数量
2. **检查脚本覆盖**：对比数据库中的 NPC 数量和脚本文件数量，找出没有脚本的 NPC
3. **维护脚本**：定期检查脚本文件，确保重要 NPC 都有对应的脚本
4. **日志监控**：关注 `logs/ScriptEx_Log.log`，及时发现脚本错误

---

## 八、相关文件

- **脚本管理器**：`src/scripting/NPCScriptManager.java`
- **NPC 工厂类**：`src/server/life/MapleLifeFactory.java`
- **NPC 定义类**：`src/server/life/MapleNPC.java`
- **脚本目录**：`scripts/scripts/npc/`
- **NPC ID 详情**：`scripts/npc id 详情.txt`
