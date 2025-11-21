package handling.channel.handler;

import client.BuddyEntry;
import client.BuddyList;
import client.CharacterNameAndId;
import client.MapleCharacter;
import client.MapleClient;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class BuddyListHandler {
    private static void nextPendingRequest(final MapleClient c) {
        final BuddyEntry pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getCharacterId(),
                    pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
        }
    }

    private static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(final String name, final String group)
            throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name LIKE ?");
        ps.setString(1, name);
        final ResultSet rs = ps.executeQuery();
        CharacterIdNameBuddyCapacity ret = null;
        if (rs.next() && rs.getInt("gm") == 0) {
            ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("level"),
                    rs.getInt("job"), group, rs.getInt("buddyCapacity"));
        }
        rs.close();
        ps.close();
        return ret;
    }

    public static void BuddyOperation(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        final BuddyList buddylist = c.getPlayer().getBuddylist();
        switch (mode) {
            case 1: {
                final String addName = slea.readMapleAsciiString();
                final String groupName = slea.readMapleAsciiString();
                final BuddyEntry ble = buddylist.get(addName);
                if (addName.length() > 13 || groupName.length() > 16) {
                    return;
                }
                if (ble != null && (ble.getGroup().equals(groupName) || !ble.isVisible())) {
                    c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 11));
                } else if (ble != null && ble.isVisible()) {
                    ble.setGroup(groupName);
                    c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                    c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 13));
                } else if (buddylist.isFull()) {
                    c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 11));
                } else {
                    try {
                        CharacterIdNameBuddyCapacity charWithId = null;
                        final int channel = World.Find.findChannel(addName);
                        MapleCharacter otherChar = null;
                        if (channel > 0) {
                            otherChar = ChannelServer.getInstance(channel).getPlayerStorage()
                                    .getCharacterByName(addName);
                            if (!otherChar.isGM() || c.getPlayer().isGM()) {
                                charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(),
                                        otherChar.getLevel(), otherChar.getJob(), groupName,
                                        otherChar.getBuddylist().getCapacity());
                            }
                        } else {
                            charWithId = getCharacterIdAndNameFromDatabase(addName, groupName);
                        }
                        if (charWithId != null) {
                            BuddyList.BuddyAddResult buddyAddResult = null;
                            if (channel > 0) {
                                buddyAddResult = World.Buddy.requestBuddyAdd(addName, c.getChannel(),
                                        c.getPlayer().getId(), c.getPlayer().getName(), c.getPlayer().getLevel(),
                                        c.getPlayer().getJob());
                            } else {
                                final Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement(
                                        "SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                                ps.setInt(1, charWithId.getId());
                                ResultSet rs = ps.executeQuery();
                                if (!rs.next()) {
                                    ps.close();
                                    rs.close();
                                    throw new RuntimeException("Result set expected");
                                }
                                final int count = rs.getInt("buddyCount");
                                if (count >= charWithId.getBuddyCapacity()) {
                                    buddyAddResult = BuddyList.BuddyAddResult.BUDDYLIST_FULL;
                                }
                                rs.close();
                                ps.close();
                                ps = con.prepareStatement(
                                        "SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                                ps.setInt(1, charWithId.getId());
                                ps.setInt(2, c.getPlayer().getId());
                                rs = ps.executeQuery();
                                if (rs.next()) {
                                    buddyAddResult = BuddyList.BuddyAddResult.ALREADY_ON_LIST;
                                }
                                rs.close();
                                ps.close();
                            }
                            if (buddyAddResult == BuddyList.BuddyAddResult.BUDDYLIST_FULL) {
                                c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 12));
                            } else {
                                int displayChannel = -1;
                                final int otherCid = charWithId.getId();
                                final int myCid = c.getPlayer().getId();
                                final Connection con2 = DatabaseConnection.getConnection();
                                
                                if (buddyAddResult == BuddyList.BuddyAddResult.ALREADY_ON_LIST && channel > 0) {
                                    displayChannel = channel;
                                    notifyRemoteChannel(c, channel, otherCid, groupName,
                                            BuddyList.BuddyOperation.ADDED);
                                    // 如果已经在列表中，确保申请者这边也有记录
                                    try {
                                        PreparedStatement psCheck = con2.prepareStatement(
                                                "SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                                        psCheck.setInt(1, myCid);
                                        psCheck.setInt(2, otherCid);
                                        ResultSet rsCheck = psCheck.executeQuery();
                                        if (!rsCheck.next()) {
                                            // 申请者这边没有记录，需要插入
                                            PreparedStatement psInsert = con2.prepareStatement(
                                                    "INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 0)");
                                            psInsert.setInt(1, myCid);
                                            psInsert.setInt(2, otherCid);
                                            psInsert.setString(3, groupName);
                                            psInsert.executeUpdate();
                                            psInsert.close();
                                        }
                                        rsCheck.close();
                                        psCheck.close();
                                    } catch (SQLException e) {
                                        System.err.println("检查/插入好友记录时出错: " + e);
                                    }
                                } else if (buddyAddResult != BuddyList.BuddyAddResult.ALREADY_ON_LIST) {
                                    // 对方不在列表上，需要插入双向记录
                                    try {
                                        // 插入对方收到的好友申请记录（pending=1）
                                        PreparedStatement psPending = con2.prepareStatement(
                                                "INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE groupname = ?");
                                        psPending.setInt(1, otherCid);
                                        psPending.setInt(2, myCid);
                                        psPending.setString(3, groupName);
                                        psPending.setString(4, groupName);
                                        psPending.executeUpdate();
                                        psPending.close();
                                        
                                        // 插入申请者这边的好友记录（pending=0，表示已添加）
                                        PreparedStatement psMy = con2.prepareStatement(
                                                "INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 0) ON DUPLICATE KEY UPDATE groupname = ?, pending = 0");
                                        psMy.setInt(1, myCid);
                                        psMy.setInt(2, otherCid);
                                        psMy.setString(3, groupName);
                                        psMy.setString(4, groupName);
                                        psMy.executeUpdate();
                                        psMy.close();
                                        
                                        if (channel > 0) {
                                            displayChannel = channel;
                                            notifyRemoteChannel(c, channel, otherCid, groupName,
                                                    BuddyList.BuddyOperation.ADDED);
                                        }
                                    } catch (SQLException e) {
                                        System.err.println("插入好友记录时出错: " + e);
                                        e.printStackTrace();
                                    }
                                }
                                buddylist.put(new BuddyEntry(charWithId.getName(), otherCid, groupName, displayChannel,
                                        true, charWithId.getLevel(), charWithId.getJob()));
                                c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 15));
                        }
                    } catch (SQLException e) {
                        System.err.println("SQL THROW" + e);
                    }
                }
                nextPendingRequest(c);
                break;
            }
            case 2: {
                final int otherCid2 = slea.readInt();
                if (!buddylist.isFull()) {
                    try {
                        final int channel = World.Find.findChannel(otherCid2);
                        String otherName = null;
                        int otherLevel = 0;
                        int otherJob = 0;
                        if (channel < 0) {
                            final Connection con3 = DatabaseConnection.getConnection();
                            final PreparedStatement ps3 = con3
                                    .prepareStatement("SELECT name, level, job FROM characters WHERE id = ?");
                            ps3.setInt(1, otherCid2);
                            final ResultSet rs2 = ps3.executeQuery();
                            if (rs2.next()) {
                                otherName = rs2.getString("name");
                                otherLevel = rs2.getInt("level");
                                otherJob = rs2.getInt("job");
                            }
                            rs2.close();
                            ps3.close();
                        } else {
                            final MapleCharacter otherChar2 = ChannelServer.getInstance(channel).getPlayerStorage()
                                    .getCharacterById(otherCid2);
                            otherName = otherChar2.getName();
                            otherLevel = otherChar2.getLevel();
                            otherJob = otherChar2.getJob();
                        }
                        if (otherName != null) {
                            buddylist.put(new BuddyEntry(otherName, otherCid2, BuddyList.DEFAULT_GROUP, channel, true,
                                    otherLevel, otherJob));
                            c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                            notifyRemoteChannel(c, channel, otherCid2, BuddyList.DEFAULT_GROUP,
                                    BuddyList.BuddyOperation.ADDED);
                        }
                    } catch (SQLException e2) {
                        System.err.println("SQL THROW" + e2);
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 11));
                }
                nextPendingRequest(c);
                break;
            }
            case 3: {
                final int otherCid2 = slea.readInt();
                final BuddyEntry blz = buddylist.get(otherCid2);
                if (blz != null && blz.isVisible()) {
                    notifyRemoteChannel(c, World.Find.findChannel(otherCid2), otherCid2, blz.getGroup(),
                            BuddyList.BuddyOperation.DELETED);
                }
                buddylist.remove(otherCid2);
                c.getSession().write(MaplePacketCreator.updateBuddylist(c.getPlayer().getBuddylist().getBuddies()));
                nextPendingRequest(c);
                break;
            }
            default: {
                System.out.println("Unknown buddylist: " + slea.toString());
                break;
            }
        }
    }

    private static void notifyRemoteChannel(final MapleClient c, final int remoteChannel, final int otherCid,
            final String group, final BuddyList.BuddyOperation operation) {
        final MapleCharacter player = c.getPlayer();
        if (remoteChannel > 0) {
            World.Buddy.buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation,
                    player.getLevel(), player.getJob(), group);
        }
    }

    private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
        private final int buddyCapacity;

        public CharacterIdNameBuddyCapacity(final int id, final String name, final int level, final int job,
                final String group, final int buddyCapacity) {
            super(id, name, level, job, group);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return this.buddyCapacity;
        }
    }
}
