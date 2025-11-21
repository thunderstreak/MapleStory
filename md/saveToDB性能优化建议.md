# saveToDB 性能优化建议

## 当前问题

虽然代码已经改为异步执行，但 `saveToDB` 仍然需要 3-4 秒才能完成，这表明数据库操作本身存在性能瓶颈。

## 性能瓶颈分析

### 1. 循环中的查询操作

`saveToDB` 方法中有多个循环，每个循环都执行查询操作：

#### questinfo 循环（第 1509-1533 行）

```java
ps = con.prepareStatement("SELECT * FROM questinfo WHERE `characterid` = ? AND `quest` = ? LIMIT 1");
for (final Map.Entry<Integer, String> q : this.questinfo.entrySet()) {
    ps.setInt(2, questID);
    rs = ps.executeQuery();  // 每个任务都要查询一次
    if (rs.next()) {
        // UPDATE
    } else {
        // INSERT
    }
}
```

**问题**：如果角色有 100 个任务，就要执行 100 次查询 + 100 次更新/插入 = 200 次数据库操作。

#### queststatus 循环（第 1535-1592 行）

```java
ps = con.prepareStatement("SELECT * FROM queststatus WHERE `characterid` = ? AND `quest` = ? LIMIT 1");
for (final MapleQuestStatus q2 : this.quests.values()) {
    rs = ps.executeQuery();  // 每个任务状态都要查询一次
    // ... 更新或插入
}
```

**问题**：同样的问题，每个任务状态都要先查询再更新/插入。

#### skills 循环（第 1594-1646 行）

```java
// 第一次循环：查询所有技能，删除不存在的
ps = con.prepareStatement("SELECT * FROM skills WHERE `characterid` = ?");
rs = ps.executeQuery();
while (rs.next()) {
    // 检查技能是否存在
}

// 第二次循环：更新或插入每个技能
ps = con.prepareStatement("SELECT * FROM skills WHERE `characterid` = ? AND `skillid` = ? LIMIT 1");
for (final Map.Entry<ISkill, SkillEntry> skill2 : this.skills.entrySet()) {
    rs = ps.executeQuery();  // 每个技能都要查询一次
    // ... 更新或插入
}
```

**问题**：技能操作需要两次循环，第一次查询所有技能，第二次对每个技能查询一次。

### 2. 数据库往返次数过多

假设一个角色有：

- 50 个任务（questinfo）
- 50 个任务状态（queststatus）
- 30 个技能（skills）
- 100 个物品

总数据库操作次数：

- characters 表：1 次 UPDATE
- skillmacros 表：最多 5 次 UPDATE
- inventoryslot 表：1 次 UPDATE
- 物品保存：可能多次（取决于 ItemLoader 实现）
- questinfo：50 次 SELECT + 50 次 UPDATE/INSERT = 100 次
- queststatus：50 次 SELECT + 50 次 UPDATE/INSERT = 100 次
- skills：1 次 SELECT（所有技能）+ 30 次 SELECT + 30 次 UPDATE/INSERT = 61 次

**总计**：可能超过 260 次数据库操作！

### 3. 事务隔离级别

```java
con.setTransactionIsolation(1);  // READ_UNCOMMITTED
con.setAutoCommit(false);
```

虽然使用了事务，但大量的数据库操作仍然会导致锁等待。

## 优化方案

### 方案 1：批量操作（推荐）

使用批量更新减少数据库往返：

```java
// 优化 questinfo 保存
ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `customData` = ?");
for (final Map.Entry<Integer, String> q : this.questinfo.entrySet()) {
    ps.setInt(1, this.id);
    ps.setInt(2, q.getKey());
    ps.setString(3, q.getValue());
    ps.setString(4, q.getValue());
    ps.addBatch();
}
ps.executeBatch();
ps.close();
```

**优点**：

- 减少数据库往返次数
- 使用 ON DUPLICATE KEY UPDATE 避免先查询
- 批量执行提高效率

### 方案 2：减少查询次数

一次性查询所有需要的数据，然后在内存中处理：

```java
// 一次性查询所有 questinfo
ps = con.prepareStatement("SELECT `quest`, `customData` FROM questinfo WHERE `characterid` = ?");
ps.setInt(1, this.id);
rs = ps.executeQuery();
final Map<Integer, String> existingQuests = new HashMap<Integer, String>();
while (rs.next()) {
    existingQuests.put(rs.getInt("quest"), rs.getString("customData"));
}
rs.close();
ps.close();

// 批量更新或插入
ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `customData` = ?");
for (final Map.Entry<Integer, String> q : this.questinfo.entrySet()) {
    if (!existingQuests.containsKey(q.getKey()) || !existingQuests.get(q.getKey()).equals(q.getValue())) {
        ps.setInt(1, this.id);
        ps.setInt(2, q.getKey());
        ps.setString(3, q.getValue());
        ps.setString(4, q.getValue());
        ps.addBatch();
    }
}
if (ps.getParameterMetaData().getParameterCount() > 0) {
    ps.executeBatch();
}
ps.close();
```

### 方案 3：优化数据库索引

确保相关表有正确的索引：

```sql
-- questinfo 表索引
CREATE INDEX idx_questinfo_char_quest ON questinfo(characterid, quest);

-- queststatus 表索引
CREATE INDEX idx_queststatus_char_quest ON queststatus(characterid, quest);

-- skills 表索引
CREATE INDEX idx_skills_char_skill ON skills(characterid, skillid);
```

### 方案 4：延迟保存非关键数据

对于不经常变化的数据，可以延迟保存：

```java
public void saveToDB(final boolean dc, final boolean fromcs) {
    // 立即保存关键数据（角色基本信息）
    saveCharacterBasicInfo();

    // 延迟保存非关键数据（任务、技能等）
    if (dc || fromcs) {
        saveAllData();  // 断开连接或离开商城时保存所有数据
    } else {
        saveCriticalDataOnly();  // 正常游戏时只保存关键数据
    }
}
```

### 方案 5：使用连接池优化

检查并优化数据库连接池配置：

```java
// 在 DatabaseConnection 中优化连接池
// - 增加连接池大小
// - 设置连接超时
// - 启用连接池监控
```

## 立即优化建议

### 1. 添加性能监控

在 `saveToDB` 方法中添加详细的性能监控：

```java
public void saveToDB(final boolean dc, final boolean fromcs) {
    final long totalStartTime = System.currentTimeMillis();
    long stepStartTime;

    try {
        // ... 原有代码 ...

        stepStartTime = System.currentTimeMillis();
        // 更新 characters 表
        // ... 代码 ...
        System.out.println("saveToDB - characters: " + (System.currentTimeMillis() - stepStartTime) + "ms");

        stepStartTime = System.currentTimeMillis();
        // 保存 questinfo
        // ... 代码 ...
        System.out.println("saveToDB - questinfo: " + (System.currentTimeMillis() - stepStartTime) + "ms");

        // ... 其他步骤 ...

    } finally {
        final long totalDuration = System.currentTimeMillis() - totalStartTime;
        if (totalDuration > 1000) {
            System.err.println("saveToDB 总耗时: " + totalDuration + "ms - 角色: " + this.getName());
        }
    }
}
```

### 2. 检查数据库慢查询

```sql
-- 启用慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 记录超过1秒的查询

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
```

### 3. 检查数据库锁等待

```sql
-- 查看当前锁等待
SHOW PROCESSLIST;

-- 查看 InnoDB 锁信息
SELECT * FROM information_schema.INNODB_LOCKS;
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

## 预期效果

实施这些优化后，预期可以将 `saveToDB` 的执行时间从 3-4 秒降低到：

- **理想情况**：< 500ms
- **正常情况**：< 1000ms
- **最坏情况**：< 2000ms

## 实施优先级

1. **高优先级**：添加性能监控，找出具体瓶颈
2. **中优先级**：优化数据库索引
3. **中优先级**：使用批量操作优化 questinfo/queststatus/skills
4. **低优先级**：实施延迟保存策略

## 注意事项

1. **数据一致性**：优化时要确保数据一致性
2. **测试**：在测试环境充分测试后再部署
3. **备份**：修改前备份数据库
4. **监控**：实施后持续监控性能
