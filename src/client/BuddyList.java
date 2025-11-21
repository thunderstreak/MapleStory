package client;

import java.io.*;
import database.*;
import java.sql.*;
import java.util.*;
import tools.*;

public class BuddyList implements Serializable {
    public static String DEFAULT_GROUP;
    private Map<Integer, BuddyEntry> buddies;
    private byte capacity;
    private Deque<BuddyEntry> pendingReqs;
    private boolean changed;

    public static int getBuddyCount(final int chrId, final int pending) {
        int count = 0;
        final Connection con = DatabaseConnection.getConnection();
        try (final PreparedStatement ps = con
                .prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = ?")) {
            ps.setInt(1, chrId);
            ps.setInt(2, pending);
            try (final ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("BuddyListHandler: getBuudyCount From DB is Error.");
                }
                count = rs.getInt("buddyCount");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return count;
    }

    public static int getBuddyCapacity(final int charId) {
        int capacity = -1;
        final Connection con = DatabaseConnection.getConnection();
        try (final PreparedStatement ps = con.prepareStatement("SELECT buddyCapacity FROM characters WHERE id = ?")) {
            ps.setInt(1, charId);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    capacity = rs.getInt("buddyCapacity");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return capacity;
    }

    public static int getBuddyPending(final int chrId, final int buddyId) {
        int pending = -1;
        final Connection con = DatabaseConnection.getConnection();
        try (final PreparedStatement ps = con
                .prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?")) {
            ps.setInt(1, chrId);
            ps.setInt(2, buddyId);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pending = rs.getInt("pending");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return pending;
    }

    public static void addBuddyToDB(final MapleCharacter player, final BuddyEntry buddy) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            try (final PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 1)")) {
                ps.setInt(1, buddy.getCharacterId());
                ps.setInt(2, player.getId());
                ps.setString(3, buddy.getGroup());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public BuddyList(final byte capacity) {
        this.pendingReqs = new LinkedList<BuddyEntry>();
        this.changed = false;
        this.buddies = new LinkedHashMap<Integer, BuddyEntry>();
        this.capacity = capacity;
    }

    public BuddyList(final int capacity) {
        this.pendingReqs = new LinkedList<BuddyEntry>();
        this.changed = false;
        this.buddies = new LinkedHashMap<Integer, BuddyEntry>();
        this.capacity = (byte) capacity;
    }

    public boolean contains(final int characterId) {
        return this.buddies.containsKey(characterId);
    }

    public boolean containsVisible(final int charId) {
        final BuddyEntry ble = this.buddies.get(charId);
        return ble != null && ble.isVisible();
    }

    public byte getCapacity() {
        return this.capacity;
    }

    public void setCapacity(final byte newCapacity) {
        this.capacity = newCapacity;
    }

    public BuddyEntry get(final int characterId) {
        return this.buddies.get(characterId);
    }

    public BuddyEntry get(final String characterName) {
        final String searchName = characterName.toLowerCase();
        for (final BuddyEntry ble : this.buddies.values()) {
            if (ble.getName().toLowerCase().equals(searchName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(final BuddyEntry newEntry) {
        this.buddies.put(newEntry.getCharacterId(), newEntry);
        this.changed = true;
    }

    public void remove(final int characterId) {
        this.buddies.remove(characterId);
        this.changed = true;
    }

    public Collection<BuddyEntry> getBuddies() {
        return this.buddies.values();
    }

    public boolean isFull() {
        return this.buddies.size() >= this.capacity;
    }

    public Collection<Integer> getBuddiesIds() {
        return this.buddies.keySet();
    }

    public void loadFromTransfer(final Map<BuddyEntry, Boolean> data) {
        for (final Map.Entry<BuddyEntry, Boolean> qs : data.entrySet()) {
            final BuddyEntry buddyid = qs.getKey();
            final boolean pair = qs.getValue();
            if (!pair) {
                this.pendingReqs.push(buddyid);
            } else {
                this.put(new BuddyEntry(buddyid.getName(), buddyid.getCharacterId(), buddyid.getGroup(), -1, true,
                        buddyid.getLevel(), buddyid.getJob()));
            }
        }
    }

    public void loadFromDb(final int characterId) throws SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            // 使用LEFT JOIN来处理好友角色可能不存在的情况
            ps = con.prepareStatement(
                    "SELECT b.buddyid, b.pending, c.name as buddyname, c.job as buddyjob, c.level as buddylevel, b.groupname FROM buddies as b LEFT JOIN characters as c ON c.id = b.buddyid WHERE b.characterid = ?");
            ps.setInt(1, characterId);
            rs = ps.executeQuery();
            int invalidCount = 0;
            while (rs.next()) {
                try {
                    final int buddyid = rs.getInt("buddyid");
                    final String buddyname = rs.getString("buddyname");
                    // 检查好友角色是否存在（使用LEFT JOIN时，如果角色不存在，buddyname会是null）
                    if (buddyname == null) {
                        System.err.println("警告：好友角色不存在或已删除 - characterid: " + characterId + ", buddyid: " + buddyid
                                + "，将删除此无效的好友记录");
                        invalidCount++;
                        // 删除无效的好友记录
                        try {
                            PreparedStatement psDelete = con.prepareStatement(
                                    "DELETE FROM buddies WHERE characterid = ? AND buddyid = ?");
                            psDelete.setInt(1, characterId);
                            psDelete.setInt(2, buddyid);
                            psDelete.executeUpdate();
                            psDelete.close();
                        } catch (SQLException e) {
                            System.err.println("删除无效好友记录时出错: " + e);
                            e.printStackTrace();
                        }
                        continue; // 跳过无效的好友关系
                    }
                    final int pending = rs.getInt("pending");
                    final String groupname = rs.getString("groupname");
                    // 处理job和level可能为null的情况
                    int buddyLevel = 0;
                    int buddyJob = 0;
                    try {
                        buddyLevel = rs.getInt("buddylevel");
                        if (rs.wasNull()) {
                            buddyLevel = 0;
                        }
                    } catch (SQLException e) {
                        buddyLevel = 0;
                    }
                    try {
                        buddyJob = rs.getInt("buddyjob");
                        if (rs.wasNull()) {
                            buddyJob = 0;
                        }
                    } catch (SQLException e) {
                        buddyJob = 0;
                    }

                    if (pending == 1) {
                        this.pendingReqs.push(new BuddyEntry(buddyname, buddyid,
                                (groupname != null ? groupname : BuddyList.DEFAULT_GROUP), -1, false,
                                buddyLevel, buddyJob));
                    } else {
                        this.put(new BuddyEntry(buddyname, buddyid,
                                (groupname != null ? groupname : BuddyList.DEFAULT_GROUP), -1, true,
                                buddyLevel, buddyJob));
                    }
                } catch (Exception e) {
                    // 处理单条记录加载错误，继续处理其他记录
                    System.err.println("加载好友记录时出错 - characterid: " + characterId + ", 错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (invalidCount > 0) {
                System.err.println("已清理 " + invalidCount + " 条无效的好友记录 - characterid: " + characterId);
            }
            // 删除待处理的好友申请（pending=1），这些应该在登录时处理
            if (ps != null) {
                ps.close();
            }
            ps = con.prepareStatement("DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("加载好友列表时发生SQL错误 - characterid: " + characterId + ", 错误: " + e.getMessage());
            e.printStackTrace();
            // 不重新抛出异常，允许角色继续加载（使用空的好友列表）
        } finally {
            // 确保资源被正确关闭
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                System.err.println("关闭数据库资源时出错: " + e);
            }
        }
    }

    public BuddyEntry pollPendingRequest() {
        return this.pendingReqs.pollLast();
    }

    public void addBuddyRequest(final MapleClient client, final int buddyId, final String buddyName,
            final int buddyChannel, final int buddyLevel, final int buddyJob) {
        this.put(
                new BuddyEntry(buddyName, buddyId, BuddyList.DEFAULT_GROUP, buddyChannel, false, buddyLevel, buddyJob));
        if (this.pendingReqs.isEmpty()) {
            client.sendPacket(MaplePacketCreator.requestBuddylistAdd(buddyId, buddyName, buddyLevel, buddyJob));
        } else {
            final BuddyEntry newPair = new BuddyEntry(buddyName, buddyId, BuddyList.DEFAULT_GROUP, -1, false, buddyJob,
                    buddyLevel);
            this.pendingReqs.push(newPair);
        }
    }

    public void setChanged(final boolean v) {
        this.changed = v;
    }

    public boolean changed() {
        return this.changed;
    }

    static {
        BuddyList.DEFAULT_GROUP = "其他";
    }

    public enum BuddyOperation {
        ADDED,
        DELETED;
    }

    public enum BuddyAddResult {
        BUDDYLIST_FULL,
        ALREADY_ON_LIST,
        OK;
    }
}
