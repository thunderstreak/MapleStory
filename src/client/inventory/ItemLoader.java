package client.inventory;

import constants.GameConstants;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.Pair;

public enum ItemLoader {
    装备道具("inventoryitems", "inventoryequipment", 0, new String[] { "characterid" }),
    STORAGE("inventoryitems", "inventoryequipment", 1, new String[] { "accountid" }),
    CASHSHOP_EXPLORER("csitems", "csequipment", 2, new String[] { "accountid" }),
    CASHSHOP_CYGNUS("csitems", "csequipment", 3, new String[] { "accountid" }),
    CASHSHOP_ARAN("csitems", "csequipment", 4, new String[] { "accountid" }),
    HIRED_MERCHANT("hiredmerchitems", "hiredmerchequipment", 5,
            new String[] { "packageid", "accountid", "characterid" }),
    DUEY("dueyitems", "dueyequipment", 6, new String[] { "packageid" }),
    CASHSHOP_EVAN("csitems", "csequipment", 7, new String[] { "accountid" }),
    MTS("mtsitems", "mtsequipment", 8, new String[] { "packageid" }),
    MTS_TRANSFER("mtstransfer", "mtstransferequipment", 9, new String[] { "characterid" }),
    CASHSHOP_DB("csitems", "csequipment", 10, new String[] { "accountid" }),
    CASHSHOP_RESIST("csitems", "csequipment", 11, new String[] { "accountid" });

    private final int value;

    private final String table;

    private final String table_equip;

    private final List<String> arg;

    ItemLoader(String table, String table_equip, int value, String... arg) {
        this.table = table;
        this.table_equip = table_equip;
        this.value = value;
        this.arg = Arrays.asList(arg);
    }

    public int getValue() {
        return this.value;
    }

    public Map<Integer, Pair<IItem, MapleInventoryType>> loadItems_hm(int packageid, int accountid)
            throws SQLException {
        Map<Integer, Pair<IItem, MapleInventoryType>> items = new LinkedHashMap<>();
        StringBuilder query = new StringBuilder();
        query.append(
                "SELECT * FROM `hiredmerchitems` LEFT JOIN `hiredmerchequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `accountid` = ? ");
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString());
        ps.setInt(1, this.value);
        ps.setInt(2, accountid);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));
            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"), rs.getInt("uniqueid"),
                        rs.getByte("flag"));
                equip.setQuantity((short) 1);
                equip.setOwner(rs.getString("owner"));
                equip.setExpiration(rs.getLong("expiredate"));
                equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                equip.setLevel(rs.getByte("level"));
                equip.setStr(rs.getShort("str"));
                equip.setDex(rs.getShort("dex"));
                equip.setInt(rs.getShort("int"));
                equip.setLuk(rs.getShort("luk"));
                equip.setHp(rs.getShort("hp"));
                equip.setMp(rs.getShort("mp"));
                equip.setWatk(rs.getShort("watk"));
                equip.setMatk(rs.getShort("matk"));
                equip.setWdef(rs.getShort("wdef"));
                equip.setMdef(rs.getShort("mdef"));
                equip.setAcc(rs.getShort("acc"));
                equip.setAvoid(rs.getShort("avoid"));
                equip.setHands(rs.getShort("hands"));
                equip.setSpeed(rs.getShort("speed"));
                equip.setJump(rs.getShort("jump"));
                equip.setViciousHammer(rs.getByte("ViciousHammer"));
                equip.setItemEXP(rs.getInt("itemEXP"));
                equip.setGMLog(rs.getString("GM_Log"));
                equip.setDurability(rs.getInt("durability"));
                equip.setEnhance(rs.getByte("enhance"));
                equip.setPotential1(rs.getShort("potential1"));
                equip.setPotential2(rs.getShort("potential2"));
                equip.setPotential3(rs.getShort("potential3"));
                equip.setHpR(rs.getShort("hpR"));
                equip.setMpR(rs.getShort("mpR"));
                equip.setGiftFrom(rs.getString("sender"));
                equip.setEquipLevel(rs.getByte("itemlevel"));
                equip.setEquipOnlyId(rs.getInt("equipOnlyId"));
                if (equip.getUniqueId() > -1 &&
                        GameConstants.isEffectRing(rs.getInt("itemid"))) {
                    MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(), mit.equals(MapleInventoryType.EQUIPPED));
                    if (ring != null)
                        equip.setRing(ring);
                }
                items.put(Integer.valueOf(rs.getInt("inventoryitemid")), new Pair(equip.copy(), mit));
                continue;
            }
            Item item = new Item(rs.getInt("itemid"), rs.getShort("position"), rs.getShort("quantity"),
                    rs.getByte("flag"));
            item.setUniqueId(rs.getInt("uniqueid"));
            item.setOwner(rs.getString("owner"));
            item.setExpiration(rs.getLong("expiredate"));
            item.setEquipOnlyId(rs.getInt("equipOnlyId"));
            item.setGMLog(rs.getString("GM_Log"));
            item.setGiftFrom(rs.getString("sender"));
            if (GameConstants.isPet(item.getItemId()))
                if (item.getUniqueId() > -1) {
                    MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(), item.getPosition());
                    if (pet != null)
                        item.setPet(pet);
                } else {
                    int new_unique = MapleInventoryIdentifier.getInstance();
                    item.setUniqueId(new_unique);
                    item.setPet(MaplePet.createPet(item.getItemId(), new_unique));
                }
            items.put(Integer.valueOf(rs.getInt("inventoryitemid")), new Pair(item.copy(), mit));
        }
        rs.close();
        ps.close();
        return items;
    }

    public Map<Integer, Pair<IItem, MapleInventoryType>> loadItems(boolean login, Integer... id) throws SQLException {
        List<Integer> lulz = Arrays.asList(id);
        Map<Integer, Pair<IItem, MapleInventoryType>> items = new LinkedHashMap<>();
        if (lulz.size() != this.arg.size())
            return items;
        // 对于STORAGE类型，验证accountid必须大于0
        if (this == STORAGE && lulz.size() > 0) {
            int accountId = ((Integer) lulz.get(0)).intValue();
            if (accountId <= 0) {
                System.err.println("[仓库错误] 无效的账号ID: " + accountId);
                return items;
            }
        }
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `");
        query.append(this.table);
        query.append("` LEFT JOIN `");
        query.append(this.table_equip);
        query.append("` USING(`inventoryitemid`) WHERE `type` = ?");
        for (String g : this.arg) {
            query.append(" AND `");
            query.append(g);
            query.append("` = ?");
        }
        // 对于STORAGE类型，额外确保accountid不为NULL且大于0
        if (this == STORAGE) {
            query.append(" AND `accountid` IS NOT NULL AND `accountid` > 0");
        }
        if (login) {
            query.append(" AND `inventorytype` = ");
            query.append(MapleInventoryType.EQUIPPED.getType());
        }
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString());
        ps.setInt(1, this.value);
        for (int i = 0; i < lulz.size(); i++)
            ps.setInt(i + 2, ((Integer) lulz.get(i)).intValue());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));
            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"), rs.getInt("uniqueid"),
                        rs.getByte("flag"));
                if (!login) {
                    equip.setQuantity((short) 1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setExpiration(rs.getLong("expiredate"));
                    equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                    equip.setLevel(rs.getByte("level"));
                    equip.setStr(rs.getShort("str"));
                    equip.setDex(rs.getShort("dex"));
                    equip.setInt(rs.getShort("int"));
                    equip.setLuk(rs.getShort("luk"));
                    equip.setHp(rs.getShort("hp"));
                    equip.setMp(rs.getShort("mp"));
                    equip.setWatk(rs.getShort("watk"));
                    equip.setMatk(rs.getShort("matk"));
                    equip.setWdef(rs.getShort("wdef"));
                    equip.setMdef(rs.getShort("mdef"));
                    equip.setAcc(rs.getShort("acc"));
                    equip.setAvoid(rs.getShort("avoid"));
                    equip.setHands(rs.getShort("hands"));
                    equip.setSpeed(rs.getShort("speed"));
                    equip.setJump(rs.getShort("jump"));
                    equip.setViciousHammer(rs.getByte("ViciousHammer"));
                    equip.setItemEXP(rs.getInt("itemEXP"));
                    equip.setGMLog(rs.getString("GM_Log"));
                    equip.setDurability(rs.getInt("durability"));
                    equip.setEnhance(rs.getByte("enhance"));
                    equip.setPotential1(rs.getShort("potential1"));
                    equip.setPotential2(rs.getShort("potential2"));
                    equip.setPotential3(rs.getShort("potential3"));
                    equip.setHpR(rs.getShort("hpR"));
                    equip.setMpR(rs.getShort("mpR"));
                    equip.setGiftFrom(rs.getString("sender"));
                    equip.setEquipLevel(rs.getByte("itemlevel"));
                    equip.setEquipOnlyId(rs.getInt("equipOnlyId"));
                    if (equip.getUniqueId() > -1 &&
                            GameConstants.isEffectRing(rs.getInt("itemid"))) {
                        MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(),
                                mit.equals(MapleInventoryType.EQUIPPED));
                        if (ring != null)
                            equip.setRing(ring);
                    }
                }
                items.put(Integer.valueOf(rs.getInt("inventoryitemid")), new Pair(equip.copy(), mit));
                continue;
            }
            Item item = new Item(rs.getInt("itemid"), rs.getShort("position"), rs.getShort("quantity"),
                    rs.getByte("flag"));
            item.setUniqueId(rs.getInt("uniqueid"));
            item.setOwner(rs.getString("owner"));
            item.setExpiration(rs.getLong("expiredate"));
            item.setEquipOnlyId(rs.getInt("equipOnlyId"));
            item.setGMLog(rs.getString("GM_Log"));
            item.setGiftFrom(rs.getString("sender"));
            if (GameConstants.isPet(item.getItemId()))
                if (item.getUniqueId() > -1) {
                    MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(), item.getPosition());
                    if (pet != null)
                        item.setPet(pet);
                } else {
                    int new_unique = MapleInventoryIdentifier.getInstance();
                    item.setUniqueId(new_unique);
                    item.setPet(MaplePet.createPet(item.getItemId(), new_unique));
                }
            items.put(Integer.valueOf(rs.getInt("inventoryitemid")), new Pair(item.copy(), mit));
        }
        rs.close();
        ps.close();
        return items;
    }

    public void saveItems(List<Pair<IItem, MapleInventoryType>> items, Integer... id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        saveItems(items, con, id);
    }

    public void saveItems(List<Pair<IItem, MapleInventoryType>> items, Connection con, Integer... id)
            throws SQLException {
        List<Integer> lulz = Arrays.asList(id);
        if (lulz.size() != this.arg.size())
            return;
        if (items == null)
            return;
        
        // 使用事务确保数据一致性
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            
            // 先删除所有相关记录
            StringBuilder deleteQuery = new StringBuilder();
            deleteQuery.append("DELETE FROM `");
            deleteQuery.append(this.table);
            deleteQuery.append("` WHERE `type` = ? AND (`");
            deleteQuery.append(this.arg.get(0));
            deleteQuery.append("` = ?");
            for (int i = 1; i < this.arg.size(); i++) {
                deleteQuery.append(" OR `");
                deleteQuery.append(this.arg.get(i));
                deleteQuery.append("` = ?");
            }
            deleteQuery.append(")");
            
            try (PreparedStatement deletePs = con.prepareStatement(deleteQuery.toString())) {
                deletePs.setInt(1, this.value);
                for (int i = 0; i < lulz.size(); i++) {
                    deletePs.setInt(i + 2, lulz.get(i));
                }
                deletePs.executeUpdate();
            }
            
            // 如果没有物品需要插入，直接提交事务
            if (items.isEmpty()) {
                con.commit();
                return;
            }
            
            // 批量插入物品
            StringBuilder insertQuery = new StringBuilder("INSERT INTO `");
            insertQuery.append(this.table);
            insertQuery.append("` (");
            for (String g : this.arg) {
                insertQuery.append(g);
                insertQuery.append(", ");
            }
            insertQuery.append(
                    "itemid, inventorytype, position, quantity, owner, GM_Log, uniqueid, expiredate, flag, `type`, sender, `equipOnlyId`) VALUES (");
            
            // 为参数占位符
            for (int i = 0; i < this.arg.size() + 12; i++) {
                insertQuery.append("?");
                if (i < this.arg.size() + 12 - 1) {
                    insertQuery.append(", ");
                }
            }
            insertQuery.append(")");
            
            try (PreparedStatement insertPs = con.prepareStatement(insertQuery.toString())) {
                for (Pair<IItem, MapleInventoryType> item : items) {
                    IItem itemTmp = item.getLeft();
                    MapleInventoryType mit = item.getRight();
                    
                    int paramIndex = 1;
                    for (int i = 0; i < lulz.size(); i++) {
                        insertPs.setInt(paramIndex++, lulz.get(i));
                    }
                    
                    insertPs.setInt(paramIndex++, itemTmp.getItemId());
                    insertPs.setInt(paramIndex++, mit.getType());
                    insertPs.setInt(paramIndex++, itemTmp.getPosition());
                    insertPs.setInt(paramIndex++, itemTmp.getQuantity());
                    insertPs.setString(paramIndex++, itemTmp.getOwner());
                    insertPs.setString(paramIndex++, itemTmp.getGMLog());
                    insertPs.setInt(paramIndex++, itemTmp.getUniqueId());
                    insertPs.setLong(paramIndex++, itemTmp.getExpiration());
                    insertPs.setByte(paramIndex++, itemTmp.getFlag());
                    insertPs.setByte(paramIndex++, (byte) this.value);
                    insertPs.setString(paramIndex++, itemTmp.getGiftFrom());
                    insertPs.setInt(paramIndex++, itemTmp.getEquipOnlyId());
                    
                    insertPs.addBatch();
                }
                
                insertPs.executeBatch();
                
                // 处理装备信息
                if (this.table_equip != null && !this.table_equip.isEmpty()) {
                    handleEquipmentInsert(con, items, insertPs);
                }
            }
            
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
    
    private void handleEquipmentInsert(Connection con, List<Pair<IItem, MapleInventoryType>> items, PreparedStatement itemInsertPs) throws SQLException {
        // 获取所有插入的ID
        List<Integer> insertedIds = new ArrayList<>();
        try (ResultSet rs = itemInsertPs.getGeneratedKeys()) {
            while (rs.next()) {
                insertedIds.add(rs.getInt(1));
            }
        }
        
        // 确保ID数量与物品数量一致
        if (insertedIds.size() != items.size()) {
            throw new SQLException("插入的物品数量与生成的ID数量不匹配");
        }
        
        // 准备装备插入语句
        String equipInsertSQL = "INSERT INTO " + this.table_equip +
                " (inventoryitemid, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, ViciousHammer, itemEXP, durability, enhance, potential1, potential2, potential3, hpR, mpR, itemlevel, equipOnlyId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement equipInsertPs = con.prepareStatement(equipInsertSQL)) {
            for (int i = 0; i < items.size(); i++) {
                Pair<IItem, MapleInventoryType> item = items.get(i);
                IItem itemTmp = item.getLeft();
                MapleInventoryType mit = item.getRight();
                
                // 只处理装备类型物品
                if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                    int inventoryItemId = insertedIds.get(i);
                    IEquip equip = (IEquip) itemTmp;
                    
                    equipInsertPs.setInt(1, inventoryItemId);
                    equipInsertPs.setInt(2, equip.getUpgradeSlots());
                    equipInsertPs.setInt(3, equip.getLevel());
                    equipInsertPs.setInt(4, equip.getStr());
                    equipInsertPs.setInt(5, equip.getDex());
                    equipInsertPs.setInt(6, equip.getInt());
                    equipInsertPs.setInt(7, equip.getLuk());
                    equipInsertPs.setInt(8, equip.getHp());
                    equipInsertPs.setInt(9, equip.getMp());
                    equipInsertPs.setInt(10, equip.getWatk());
                    equipInsertPs.setInt(11, equip.getMatk());
                    equipInsertPs.setInt(12, equip.getWdef());
                    equipInsertPs.setInt(13, equip.getMdef());
                    equipInsertPs.setInt(14, equip.getAcc());
                    equipInsertPs.setInt(15, equip.getAvoid());
                    equipInsertPs.setInt(16, equip.getHands());
                    equipInsertPs.setInt(17, equip.getSpeed());
                    equipInsertPs.setInt(18, equip.getJump());
                    equipInsertPs.setInt(19, equip.getViciousHammer());
                    equipInsertPs.setInt(20, equip.getItemEXP());
                    equipInsertPs.setInt(21, equip.getDurability());
                    equipInsertPs.setByte(22, equip.getEnhance());
                    equipInsertPs.setInt(23, equip.getPotential1());
                    equipInsertPs.setInt(24, equip.getPotential2());
                    equipInsertPs.setInt(25, equip.getPotential3());
                    equipInsertPs.setInt(26, equip.getHpR());
                    equipInsertPs.setInt(27, equip.getMpR());
                    equipInsertPs.setByte(28, equip.getEquipLevel());
                    equipInsertPs.setInt(29, equip.getEquipOnlyId());
                    
                    equipInsertPs.addBatch();
                }
            }
            
            // 执行批量插入
            equipInsertPs.executeBatch();
        }
    }
}
