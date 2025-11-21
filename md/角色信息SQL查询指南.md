# 角色信息 SQL 查询指南

## 数据库表：`characters`

### 主要字段说明

根据代码分析，`characters` 表包含以下主要字段：

#### 基本信息

- `id` - 角色 ID（主键）
- `name` - 角色名称
- `accountid` - 账号 ID（关联 `accounts` 表）
- `world` - 世界 ID（服务器 ID）

#### 角色属性

- `level` - 等级
- `job` - 职业 ID
- `exp` - 经验值
- `meso` - 金币数量
- `fame` - 人气值

#### 能力值

- `str` - 力量
- `dex` - 敏捷
- `int` - 智力
- `luk` - 运气
- `ap` - 剩余能力点
- `hpApUsed` - 已使用的 HP 能力点

#### 生命值/魔法值

- `hp` - 当前 HP
- `mp` - 当前 MP
- `maxhp` - 最大 HP
- `maxmp` - 最大 MP

#### 外观

- `hair` - 发型 ID
- `face` - 脸型 ID
- `gender` - 性别（0=男性，1=女性）
- `skincolor` - 肤色

#### 位置信息

- `map` - 当前地图 ID
- `spawnpoint` - 出生点

#### 其他信息

- `gm` - GM 等级（0=普通玩家，>0=GM）
- `guildid` - 家族 ID
- `guildrank` - 家族职位
- `party` - 组队 ID
- `marriageId` - 结婚对象 ID
- `beans` - 豆豆数量
- `vip` - VIP 等级
- `vipexpired` - VIP 到期时间
- `pvpKills` - PVP 击杀数
- `pvpDeaths` - PVP 死亡数
- `pvpVictory` - PVP 胜利数

## 常用查询语句

### 1. 根据角色名查询

```sql
-- 查询指定角色名的完整信息
SELECT * FROM characters WHERE name = '角色名';

-- 查询指定角色名的基础信息
SELECT id, name, accountid, level, job, exp, meso, fame, map, world
FROM characters
WHERE name = '角色名';
```

### 2. 根据角色 ID 查询

```sql
-- 查询指定角色ID的完整信息
SELECT * FROM characters WHERE id = 角色ID;

-- 查询指定角色ID的基础信息
SELECT id, name, accountid, level, job, exp, meso, fame, map
FROM characters
WHERE id = 角色ID;
```

### 3. 根据账号 ID 查询

```sql
-- 查询指定账号下的所有角色
SELECT * FROM characters WHERE accountid = 账号ID;

-- 查询指定账号下的所有角色（按等级排序）
SELECT id, name, level, job, exp, meso, fame, map
FROM characters
WHERE accountid = 账号ID
ORDER BY level DESC;
```

### 4. 关联账号表查询

```sql
-- 查询角色信息及账号信息
SELECT
    c.id AS 角色ID,
    c.name AS 角色名,
    c.level AS 等级,
    c.job AS 职业,
    c.meso AS 金币,
    a.name AS 账号名,
    a.banned AS 封号状态,
    a.banreason AS 封号原因
FROM characters c
LEFT JOIN accounts a ON c.accountid = a.id
WHERE c.name = '角色名';
```

### 5. 查询排行榜

```sql
-- 等级排行榜（前10名）
SELECT id, name, level, job, exp, fame
FROM characters
WHERE gm = 0
ORDER BY level DESC, exp DESC
LIMIT 10;

-- 人气排行榜（前10名）
SELECT id, name, level, fame
FROM characters
WHERE gm = 0
ORDER BY fame DESC
LIMIT 10;

-- 金币排行榜（前10名）
SELECT id, name, level, meso
FROM characters
WHERE gm = 0
ORDER BY meso DESC
LIMIT 10;
```

### 6. 查询特定条件

```sql
-- 查询指定等级以上的角色
SELECT id, name, level, job, exp
FROM characters
WHERE level >= 100
ORDER BY level DESC;

-- 查询指定职业的角色
SELECT id, name, level, job
FROM characters
WHERE job = 职业ID;

-- 查询指定地图的角色
SELECT id, name, level, map
FROM characters
WHERE map = 地图ID;

-- 查询VIP玩家
SELECT id, name, level, vip, vipexpired
FROM characters
WHERE vip > 0
ORDER BY vip DESC, level DESC;
```

### 7. 统计查询

```sql
-- 统计角色总数
SELECT COUNT(*) AS 角色总数 FROM characters;

-- 统计在线角色数（需要结合其他表或日志判断）
SELECT COUNT(*) AS 角色数 FROM characters;

-- 按等级段统计
SELECT
    CASE
        WHEN level < 10 THEN '1-9级'
        WHEN level < 30 THEN '10-29级'
        WHEN level < 50 THEN '30-49级'
        WHEN level < 70 THEN '50-69级'
        WHEN level < 100 THEN '70-99级'
        ELSE '100级以上'
    END AS 等级段,
    COUNT(*) AS 人数
FROM characters
WHERE gm = 0
GROUP BY 等级段
ORDER BY MIN(level);

-- 按职业统计
SELECT job, COUNT(*) AS 人数
FROM characters
WHERE gm = 0
GROUP BY job
ORDER BY 人数 DESC;
```

### 8. 查询角色详细信息（包含账号信息）

```sql
-- 查询角色详细信息，包含账号状态
SELECT
    c.id AS 角色ID,
    c.name AS 角色名,
    c.level AS 等级,
    c.job AS 职业,
    c.exp AS 经验,
    c.meso AS 金币,
    c.fame AS 人气,
    c.str AS 力量,
    c.dex AS 敏捷,
    c.int AS 智力,
    c.luk AS 运气,
    c.ap AS 剩余能力点,
    c.hp AS 当前HP,
    c.mp AS 当前MP,
    c.maxhp AS 最大HP,
    c.maxmp AS 最大MP,
    c.map AS 地图ID,
    c.guildid AS 家族ID,
    c.party AS 组队ID,
    c.vip AS VIP等级,
    c.pvpKills AS PVP击杀,
    c.pvpDeaths AS PVP死亡,
    c.pvpVictory AS PVP胜利,
    a.name AS 账号名,
    a.banned AS 封号状态,
    a.banreason AS 封号原因,
    a.loggedin AS 登录状态
FROM characters c
LEFT JOIN accounts a ON c.accountid = a.id
WHERE c.name = '角色名';
```

### 9. 查询角色能力值详情

```sql
-- 查询角色能力值
SELECT
    name AS 角色名,
    level AS 等级,
    job AS 职业,
    str AS 力量,
    dex AS 敏捷,
    int AS 智力,
    luk AS 运气,
    ap AS 剩余能力点,
    hpApUsed AS 已用HP能力点,
    (str + dex + int + luk + ap) AS 总能力点
FROM characters
WHERE name = '角色名';
```

### 10. 查询角色位置信息

```sql
-- 查询角色当前位置
SELECT
    name AS 角色名,
    level AS 等级,
    map AS 地图ID,
    spawnpoint AS 出生点,
    world AS 世界ID
FROM characters
WHERE name = '角色名';
```

### 11. 查询角色家族信息

```sql
-- 查询角色家族信息
SELECT
    c.name AS 角色名,
    c.level AS 等级,
    c.guildid AS 家族ID,
    c.guildrank AS 家族职位,
    g.name AS 家族名
FROM characters c
LEFT JOIN guilds g ON c.guildid = g.guildid
WHERE c.name = '角色名';
```

### 12. 查询角色组队信息

```sql
-- 查询角色组队信息
SELECT
    c.name AS 角色名,
    c.party AS 组队ID,
    COUNT(p.id) AS 组队人数
FROM characters c
LEFT JOIN characters p ON c.party = p.party AND c.party >= 0
WHERE c.name = '角色名'
GROUP BY c.name, c.party;
```

### 13. 批量查询

```sql
-- 查询多个角色（使用IN）
SELECT id, name, level, job, meso
FROM characters
WHERE name IN ('角色名1', '角色名2', '角色名3');

-- 查询指定账号下的所有角色
SELECT id, name, level, job, exp, meso, fame
FROM characters
WHERE accountid = 账号ID
ORDER BY level DESC;
```

### 14. 模糊查询

```sql
-- 根据角色名模糊查询
SELECT id, name, level, job
FROM characters
WHERE name LIKE '%关键词%'
ORDER BY level DESC
LIMIT 20;
```

### 15. 查询角色 PVP 信息

```sql
-- 查询角色PVP统计
SELECT
    name AS 角色名,
    level AS 等级,
    pvpKills AS 击杀数,
    pvpDeaths AS 死亡数,
    pvpVictory AS 胜利数,
    CASE
        WHEN (pvpKills + pvpDeaths) > 0
        THEN ROUND(pvpKills * 100.0 / (pvpKills + pvpDeaths), 2)
        ELSE 0
    END AS 胜率
FROM characters
WHERE pvpKills > 0 OR pvpDeaths > 0
ORDER BY pvpKills DESC
LIMIT 20;
```

## 常用字段说明

### 职业 ID（job）参考

- `0` = 新手
- `100-200` = 战士系
- `200-300` = 法师系
- `300-400` = 弓箭手系
- `400-500` = 飞侠系
- `500-600` = 海盗系
- `1000+` = 特殊职业（如骑士团、战神等）

### 性别（gender）

- `0` = 男性
- `1` = 女性

### GM 等级（gm）

- `0` = 普通玩家
- `> 0` = GM（数值越大权限越高）

### 封号状态（accounts.banned）

- `0` = 正常
- `1` = 手动封号
- `2` = 系统自动封号

## 实用查询示例

### 示例 1：查询玩家完整信息

```sql
SELECT
    c.id,
    c.name,
    c.level,
    c.job,
    c.exp,
    c.meso,
    c.fame,
    c.str,
    c.dex,
    c.int,
    c.luk,
    c.ap,
    c.hp,
    c.mp,
    c.maxhp,
    c.maxmp,
    c.map,
    c.guildid,
    c.vip,
    a.name AS 账号名,
    a.banned AS 封号状态,
    a.banreason AS 封号原因
FROM characters c
LEFT JOIN accounts a ON c.accountid = a.id
WHERE c.name = '角色名';
```

### 示例 2：查询账号下所有角色

```sql
SELECT
    c.id AS 角色ID,
    c.name AS 角色名,
    c.level AS 等级,
    c.job AS 职业,
    c.meso AS 金币,
    c.fame AS 人气,
    c.map AS 地图ID
FROM characters c
WHERE c.accountid = (
    SELECT id FROM accounts WHERE name = '账号名'
)
ORDER BY c.level DESC;
```

### 示例 3：查询在线玩家（需要结合其他信息判断）

```sql
-- 注意：此查询需要结合 accounts.loggedin 字段判断
SELECT
    c.id,
    c.name,
    c.level,
    c.map,
    a.loggedin AS 登录状态
FROM characters c
LEFT JOIN accounts a ON c.accountid = a.id
WHERE a.loggedin = 2  -- 2表示已登录
ORDER BY c.level DESC;
```

## 注意事项

1. **角色名区分大小写**：MySQL 默认情况下，字符串比较可能区分大小写，建议使用 `COLLATE utf8_general_ci` 或使用 `LOWER()` 函数

2. **性能优化**：如果经常查询，建议在 `name` 和 `accountid` 字段上创建索引

3. **数据安全**：查询时注意不要暴露敏感信息（如密码等）

4. **关联查询**：使用 `LEFT JOIN` 可以确保即使没有关联数据也能返回角色信息

## 相关表

- `accounts` - 账号表
- `guilds` - 家族表
- `queststatus` - 任务状态表
- `inventoryslot` - 背包槽位表
- `achievements` - 成就表
