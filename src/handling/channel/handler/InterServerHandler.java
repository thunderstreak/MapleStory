package handling.channel.handler;

import client.BuddyEntry;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import database.DatabaseConnection;
import handling.MaplePacket;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.world.CharacterIdChannelPair;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.guild.MapleGuild;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.ServerProperties;
import server.maps.FieldLimitType;
import tools.DateUtil;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.FamilyPacket;

public class InterServerHandler {
    public static void EnterCS(final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer().getMap().getId() != 180000001) {
            if (!c.getChannelServer().WarpCSShop()) {
                try {
                    final String[] socket = c.getChannelServer().getIP().split(":");
                    if (c.getPlayer().getBuffedValue(MapleBuffStat.召唤兽) != null) {
                        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.召唤兽);
                    }
                    final ChannelServer ch = ChannelServer.getInstance(c.getChannel());
                    chr.changeRemoval();
                    if (chr.getMessenger() != null) {
                        final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                        World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
                    }
                    PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
                    PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
                    PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
                    World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
                    ch.removePlayer(chr);
                    c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
                    c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]),
                            Integer.parseInt(CashShopServer.getIP().split(":")[1])));
                    chr.saveToDB(false, false);
                    chr.getMap().removePlayer(chr);
                    c.getPlayer().expirationTask(true, false);
                    c.setPlayer(null);
                    c.setReceiving(false);
                } catch (UnknownHostException ex) {
                    Logger.getLogger(InterServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                NPCScriptManager.getInstance().start(c, 9900004, 9999);
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getPlayer().dropMessage(1, "你在小黑屋里，无法进行任何操作");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void EnterMTS(final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer().getMap().getId() != 180000001) {
            if (!c.getChannelServer().WarpMTS()) {
                try {
                    final String[] socket = c.getChannelServer().getIP().split(":");
                    if (c.getPlayer().getBuffedValue(MapleBuffStat.召唤兽) != null) {
                        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.召唤兽);
                    }
                    final ChannelServer ch = ChannelServer.getInstance(c.getChannel());
                    chr.changeRemoval();
                    if (chr.getMessenger() != null) {
                        final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                        World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
                    }
                    PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
                    PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
                    PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
                    World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -20);
                    ch.removePlayer(chr);
                    c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
                    c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]),
                            Integer.parseInt(CashShopServer.getIP().split(":")[1])));
                    chr.saveToDB(false, false);
                    chr.getMap().removePlayer(chr);
                    c.setPlayer(null);
                    c.setReceiving(false);
                } catch (UnknownHostException ex) {
                    Logger.getLogger(InterServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                NPCScriptManager.getInstance().dispose(c);
                if (c.getPlayer().getTrade() != null) {
                    c.getPlayer().dropMessage(1, "交易中无法进行其他操作！");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (c.getPlayer().getLevel() >= 1) {
                    NPCScriptManager.getInstance().start(c, 9900004);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else {
                    c.getSession().write(MaplePacketCreator.getNPCTalk(9900004, (byte) 0, "玩家你好.等级不足1级无法使用快捷功能.",
                            "00 00", (byte) 0));
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        } else {
            c.getPlayer().dropMessage(1, "你在小黑屋里，无法进行任何操作");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static List<Integer> getSameAccountOtherCharID(final int charid) {
        try {
            int accountid = 0;
            final List<Integer> IDs = new ArrayList<Integer>();
            final Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = null;
            ResultSet rs = null;
            ps = con.prepareStatement("select * from characters where id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            if (accountid == 0) {
                return null;
            }
            ps = con.prepareStatement("select * from characters where accountid = ? and id != ?");
            ps.setInt(1, accountid);
            ps.setInt(2, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                IDs.add(rs.getInt("id"));
            }
            rs.close();
            ps.close();
            if (!IDs.isEmpty()) {
                return IDs;
            }
            return null;
        } catch (SQLException ex) {
            Logger.getLogger(InterServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void Loggedin(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        final List<Integer> IDs = getSameAccountOtherCharID(playerid);
        if (IDs != null && IDs.size() > 0) {
            for (final int id : IDs) {
                channelServer.getPlayerStorage().deregisterPendingPlayer(id);
            }
        }
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean firstLoggedIn = true;
        MapleCharacter player;
        long loadStartTime = System.currentTimeMillis();
        if (transfer == null) {
            System.out.println("开始加载角色数据 - 角色ID: " + playerid);
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            long loadDuration = System.currentTimeMillis() - loadStartTime;
            System.out.println(
                    "角色数据加载完成 - 角色ID: " + playerid + " 名字: " + player.getName() + " 耗时: " + loadDuration + "ms");
            if (loadDuration > 1000) {
                System.err.println("警告：loadCharFromDB 执行时间过长: " + loadDuration + "ms - 角色: " + player.getName());
            }
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
            firstLoggedIn = false;
            long loadDuration = System.currentTimeMillis() - loadStartTime;
            System.out.println(
                    "角色数据重构完成 - 角色ID: " + playerid + " 名字: " + player.getName() + " 耗时: " + loadDuration + "ms");
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());
        long accountDataStartTime = System.currentTimeMillis();
        c.loadAccountData(player.getAccountID());
        long accountDataDuration = System.currentTimeMillis() - accountDataStartTime;
        if (accountDataDuration > 500) {
            System.out.println("loadAccountData 耗时: " + accountDataDuration + "ms - 角色: " + player.getName());
        }
        ChannelServer.forceRemovePlayerByAccId(c, c.getAccID());
        final int state = c.getLoginState();
        boolean allowLogin = true; // 默认允许登录，除非检测到真正的问题
        String allowLoginTip = null;

        // 记录登录状态用于调试
        System.out.println("玩家登录 - 角色ID: " + player.getId() + " 名字: " + player.getName() + " 登录状态: " + state);

        // 只有在特定状态下才需要检查是否已有角色在线
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL
                || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            final List<String> charNames = c.loadCharacterNames(c.getWorld());
            System.out.println("检查账号下角色列表: " + charNames);

            // 再次检查并清理无效玩家（多次清理确保彻底）
            for (int cleanupRound = 0; cleanupRound < 3; cleanupRound++) {
                boolean foundInvalid = false;
                for (final String charName : charNames) {
                    for (final ChannelServer cs : ChannelServer.getAllInstances()) {
                        final MapleCharacter existingChr = cs.getPlayerStorage().getCharacterByName(charName);
                        if (existingChr != null) {
                            // 如果玩家存在但客户端无效，强制清理
                            if (existingChr.getClient() == null || !existingChr.getClient().isLoggedIn()
                                    || existingChr.getClient().getSession() == null
                                    || !existingChr.getClient().getSession().isConnected()) {
                                try {
                                    // 如果玩家有组队，先清理组队状态
                                    if (existingChr.getParty() != null) {
                                        final MapleParty party = existingChr.getParty();
                                        party.removeMember(new MaplePartyCharacter(existingChr));
                                        existingChr.setParty(null);
                                        final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                                                + " || 组队ID：" + party.getId()
                                                + " || 玩家名字：" + existingChr.getName()
                                                + " || 玩家ID：" + existingChr.getId()
                                                + " || 操作类型：登录时清理无效玩家的组队状态"
                                                + " || 错误原因：玩家存在但客户端无效，清理组队状态"
                                                + " || 频道：" + cs.getChannel()
                                                + " || 地图："
                                                + (existingChr.getMap() != null ? existingChr.getMap().getId() : "未知")
                                                + "\r\n";
                                        FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                                    }
                                    cs.removePlayer(existingChr.getId(), existingChr.getName());
                                    World.Find.forceDeregister(existingChr.getId(), existingChr.getName());
                                    foundInvalid = true;
                                    System.out.println("清理无效玩家 (第" + (cleanupRound + 1) + "轮): " + charName);
                                    final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                                            + " || 玩家名字：" + charName
                                            + " || 玩家ID：" + existingChr.getId()
                                            + " || 操作类型：登录时清理无效玩家 (第" + (cleanupRound + 1) + "轮)"
                                            + " || 错误原因：玩家存在但客户端无效，已强制清理"
                                            + " || 频道：" + cs.getChannel()
                                            + " || 地图："
                                            + (existingChr.getMap() != null ? existingChr.getMap().getId() : "未知")
                                            + "\r\n";
                                    FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                                } catch (Exception e) {
                                    System.err.println("清理无效玩家失败: " + charName + " - " + e.getMessage());
                                    final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                                            + " || 玩家名字：" + charName
                                            + " || 玩家ID：" + (existingChr != null ? existingChr.getId() : "未知")
                                            + " || 操作类型：登录时清理无效玩家失败"
                                            + " || 错误原因：" + e.getMessage()
                                            + " || 频道：" + cs.getChannel()
                                            + "\r\n";
                                    FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                                }
                            }
                        }
                    }
                }
                // 如果这一轮没有发现无效玩家，提前结束
                if (!foundInvalid) {
                    break;
                }
                // 等待一小段时间让清理完成
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 清理后再次检查
            allowLogin = !World.isCharacterListConnected(charNames);
            if (!allowLogin) {
                allowLoginTip = World.getAllowLoginTip(charNames);
                System.out.println("登录被阻止 - 原因: " + allowLoginTip);
            } else {
                System.out.println("登录检查通过 - 角色ID: " + player.getId() + " 名字: " + player.getName());
            }
        } else {
            // 如果状态不是这三个值之一，允许登录（可能是其他合法的登录状态）
            System.out.println("登录状态不是标准状态，允许登录 - 状态: " + state);
        }

        if (!allowLogin) {
            final String msg = "检测账号下已有角色登陆游戏 服务端断开这个连接 [角色ID: " + player.getId() + " 名字: " + player.getName()
                    + " ]\r\n" + allowLoginTip;
            System.out.println("自动断开连接2: " + msg);
            final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                    + " || 玩家名字：" + player.getName()
                    + " || 玩家ID：" + player.getId()
                    + " || 操作类型：登录被阻止"
                    + " || 错误原因：" + allowLoginTip
                    + " || 登录状态：" + state
                    + "\r\n";
            FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        long loginProcessStartTime = System.currentTimeMillis();
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        long addPlayerStartTime = System.currentTimeMillis();
        channelServer.addPlayer(player);
        long addPlayerDuration = System.currentTimeMillis() - addPlayerStartTime;
        if (addPlayerDuration > 500) {
            System.out.println("addPlayer 耗时: " + addPlayerDuration + "ms - 角色: " + player.getName());
        }

        long packetStartTime = System.currentTimeMillis();
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        if (player.isGM()) {
            SkillFactory.getSkill(9001004).getEffect(1).applyTo(player);
        }
        c.getSession().write(MaplePacketCreator.temporaryStats_Reset());
        long packetDuration = System.currentTimeMillis() - packetStartTime;
        if (packetDuration > 500) {
            System.out.println("发送登录封包耗时: " + packetDuration + "ms - 角色: " + player.getName());
        }

        long mapAddStartTime = System.currentTimeMillis();
        if (player.getMap() == null) {
            System.err.println("错误：玩家地图为 null - 角色: " + player.getName() + " 地图ID: " + player.getMapId());
        } else {
            player.getMap().addPlayer(player);
            long mapAddDuration = System.currentTimeMillis() - mapAddStartTime;
            if (mapAddDuration > 500) {
                System.out.println("addPlayerToMap 耗时: " + mapAddDuration + "ms - 角色: " + player.getName() + " 地图: "
                        + player.getMapId());
            }
        }
        long loginProcessDuration = System.currentTimeMillis() - loginProcessStartTime;
        System.out.println("登录流程总耗时: " + loginProcessDuration + "ms - 角色: " + player.getName());
        try {
            long buffStartTime = System.currentTimeMillis();
            player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
            player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
            player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));
            long buffDuration = System.currentTimeMillis() - buffStartTime;
            if (buffDuration > 500) {
                System.out.println("恢复Buff/Cooldown耗时: " + buffDuration + "ms - 角色: " + player.getName());
            }

            long buddyStartTime = System.currentTimeMillis();
            final Collection<Integer> buddyIds = player.getBuddylist().getBuddiesIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds, player.getGMLevel(),
                    player.isHidden());
            long buddyDuration = System.currentTimeMillis() - buddyStartTime;
            if (buddyDuration > 500) {
                System.out.println("Buddy登录处理耗时: " + buddyDuration + "ms - 角色: " + player.getName());
            }
            // 登录时清理组队中的无效成员
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                final List<MaplePartyCharacter> membersToRemove = new ArrayList<>();
                for (final MaplePartyCharacter partychar : party.getMembers()) {
                    // 跳过当前玩家自己
                    if (partychar.getId() == player.getId()) {
                        continue;
                    }
                    final int ch = World.Find.findChannel(partychar.getName());
                    if (ch <= 0) {
                        // 玩家不在任何频道，标记为需要移除
                        membersToRemove.add(partychar);
                        continue;
                    }
                    final MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage()
                            .getCharacterByName(partychar.getName());
                    if (chr == null) {
                        // 玩家不存在，标记为需要移除
                        membersToRemove.add(partychar);
                        continue;
                    }
                    // 检查客户端是否有效
                    if (chr.getClient() == null || !chr.getClient().isLoggedIn()
                            || chr.getClient().getSession() == null
                            || !chr.getClient().getSession().isConnected()) {
                        // 客户端无效，标记为需要移除
                        membersToRemove.add(partychar);
                        final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                                + " || 组队ID：" + party.getId()
                                + " || 玩家名字：" + partychar.getName()
                                + " || 玩家ID：" + partychar.getId()
                                + " || 操作类型：登录时清理无效组队成员"
                                + " || 错误原因：客户端无效或已断开连接"
                                + " || 频道：" + ch
                                + " || 地图：" + (chr.getMap() != null ? chr.getMap().getId() : "未知")
                                + "\r\n";
                        FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                    }
                }
                // 移除无效成员
                for (final MaplePartyCharacter member : membersToRemove) {
                    party.removeMember(member);
                    final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                            + " || 组队ID：" + party.getId()
                            + " || 玩家名字：" + member.getName()
                            + " || 玩家ID：" + member.getId()
                            + " || 操作类型：登录时清理无效组队成员"
                            + " || 错误原因：已从组队中移除无效成员"
                            + " || 当前登录玩家：" + player.getName()
                            + "\r\n";
                    FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                }
                // 如果组队中只剩下当前玩家，清理组队状态
                if (party.getMembers().size() <= 1) {
                    player.setParty(null);
                    final String logMsg = "时间：" + FileoutputUtil.CurrentReadable_Time()
                            + " || 组队ID：" + party.getId()
                            + " || 玩家名字：" + player.getName()
                            + " || 玩家ID：" + player.getId()
                            + " || 操作类型：登录时清理空组队"
                            + " || 错误原因：组队中只剩下当前玩家，自动清理组队状态"
                            + "\r\n";
                    FileoutputUtil.packetLog("logs/组队掉线.log", logMsg);
                } else {
                    // 更新组队状态
                    World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF,
                            new MaplePartyCharacter(player));
                }
            }
            final CharacterIdChannelPair[] multiBuddyFind;
            final CharacterIdChannelPair[] onlineBuddies = multiBuddyFind = World.Find.multiBuddyFind(player.getId(),
                    buddyIds);
            for (final CharacterIdChannelPair onlineBuddy : multiBuddyFind) {
                final BuddyEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.sendPacket(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<MaplePacket> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (final MaplePacket pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                }
            }
            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Login_Error, e);
        }
        c.getSession().write(FamilyPacket.getFamilyData());
        for (final MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        final BuddyEntry pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist()
                    .put(new BuddyEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getCharacterId(), "ETC", -1,
                            false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
            c.sendPacket(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getCharacterId(),
                    pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
        }
        player.expirationTask();
        if (player.getJob() == 132) {
            player.checkBerserk();
        }
        player.sendMacros();
        c.getSession().write(MaplePacketCreator.showCharCash(c.getPlayer()));
        player.getClient().getSession().write(MaplePacketCreator.serverMessage(channelServer.getServerMessage()));
        player.showNote();
        player.updatePartyMemberHP();
        player.startFairySchedule(false);
        player.updatePetEquip();
        player.baseSkills();
        player.spawnSavedPets();
        c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        c.getSession().write(MaplePacketCreator.weirdStatUpdate());
        if (firstLoggedIn) {
            if (player.getGMLevel() == 0) {
                if (player.getGender() == 0) {
                    World.Broadcast
                            .broadcastSmega(MaplePacketCreator
                                    .serverNotice(11, c.getChannel(),
                                            "[登录公告] 【帅哥】" + c.getPlayer().getName() + " : "
                                                    + new StringBuilder().append("进入游戏，大家热烈欢迎他吧！！！").toString())
                                    .getBytes());
                } else {
                    World.Broadcast
                            .broadcastSmega(MaplePacketCreator
                                    .serverNotice(11, c.getChannel(),
                                            "[登录公告] 【美女】" + c.getPlayer().getName() + " : "
                                                    + new StringBuilder().append("进入游戏，大家热烈欢迎她吧！！！").toString())
                                    .getBytes());
                }
            } else {
                int p = 0;
                for (final ChannelServer cserv : ChannelServer.getAllInstances()) {
                    for (final MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        if (chr != null) {
                            ++p;
                        }
                    }
                }
                player.dropMessage(6, "[服务器信息]：尊敬的管理员，欢迎进入游戏。当前在线人数：" + p + "人");
            }
        }
        // 注释掉：检查并提示玩家使用神秘箱子领取新手礼包
        if (player.haveItem(2022336)) {
            player.dropMessage(5,
                    "欢迎来到" + ServerProperties.getProperty("RoyMS.ServerName") + ",请按“I”键，打开背包，双击使用神秘箱子，领取新人礼包");
        }

        final int 阴森世界地图 = 551030200;
        if (c.getPlayer().getHp() != 50
                && (c.getPlayer().getBossLog("狮熊Boss") >= 1 || c.getPlayer().getBossLogChannel("狮熊Boss") > 0)
                && c.getPlayer().getMap().getId() != 阴森世界地图 && c.getPlayer().获取怪物数量(阴森世界地图) >= 1
                && c.getPlayer().getBossLogChannel("狮熊Boss") == c.getChannel()) {
            c.getPlayer().changeMap(阴森世界地图);
        } else {
            c.getPlayer().resetBossLog("狮熊Boss");
        }
        final int 树精地图 = 541020800;
        if (c.getPlayer().getHp() != 50
                && (c.getPlayer().getBossLog("树精Boss") >= 1 || c.getPlayer().getBossLogChannel("树精Boss") > 0)
                && c.getPlayer().getMap().getId() != 树精地图 && c.getPlayer().获取怪物数量(树精地图) >= 0
                && c.getPlayer().getBossLogChannel("树精Boss") == c.getChannel()) {
            c.getPlayer().changeMap(树精地图);
        } else {
            c.getPlayer().resetBossLog("树精Boss");
        }
        final int 普通黑龙地图阶段1 = 240060000;
        final int 普通黑龙地图阶段2 = 240060100;
        final int 普通黑龙地图阶段3 = 240060200;
        if (c.getPlayer().getHp() != 50
                && (c.getPlayer().getBossLog("普通黑龙") >= 1 || c.getPlayer().getBossLogChannel("普通黑龙") > 0)) {
            final int type = c.getPlayer().getBossLogType("普通黑龙");
            int mapID = 0;
            switch (type) {
                case 1: {
                    mapID = 普通黑龙地图阶段1;
                    break;
                }
                case 2: {
                    mapID = 普通黑龙地图阶段2;
                    break;
                }
                case 3: {
                    mapID = 普通黑龙地图阶段3;
                    break;
                }
            }
            if (c.getPlayer().getMap().getId() != mapID && c.getPlayer().getBossLogChannel("普通黑龙") == c.getChannel()) {
                int preheadCheck = 4;
                if (type == 1) {
                    if (c.getPlayer().获取怪物数量(mapID) >= 1) {
                        preheadCheck = 2;
                    } else {
                        preheadCheck = 0;
                    }
                }
                if (type == 2) {
                    if (c.getPlayer().获取怪物数量(mapID) >= 1) {
                        preheadCheck = 4;
                    } else {
                        preheadCheck = 2;
                    }
                }
                final EventManager em = c.getChannelServer().getEventSM().getEventManager("HorntailBattle");
                EventInstanceManager eim = em.getInstance("HorntailBattle");
                if (eim == null) {
                    eim = em.newInstance("HorntailBattle");
                    eim.startEventTimer(43200000L);
                    eim.schedule("CheckHorntailHead", 3000L);
                }
                em.setProperty("state", "" + type);
                em.setProperty("preheadCheck", "" + preheadCheck);
                c.getPlayer().changeMap(mapID);
            } else {
                c.getPlayer().resetBossLog("普通黑龙");
            }
        } else {
            c.getPlayer().resetBossLog("普通黑龙");
        }
        final int 扎昆祭台地图 = 280030000;
        if (c.getPlayer().getHp() != 50
                && (c.getPlayer().getBossLog("普通扎昆") >= 1 || c.getPlayer().getBossLogChannel("普通扎昆") > 0)
                && c.getPlayer().getMap().getId() != 扎昆祭台地图 && c.getPlayer().获取怪物数量(扎昆祭台地图) >= 1
                && c.getPlayer().getBossLogChannel("普通扎昆") == c.getChannel()) {
            c.getPlayer().changeMap(扎昆祭台地图);
        } else {
            c.getPlayer().resetBossLog("普通扎昆");
        }
        player.checkCopyItems();
        System.out.println("login: " + DateUtil.getCurrentDateStr() + "[服务端-用户:][名字:" + c.getPlayer().getName()
                + "][  等级:" + c.getPlayer().getLevel() + "] 进入游戏.");
    }

    public static void ChangeChannel(final SeekableLittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (c.getPlayer().getTrade() != null || !chr.isAlive() || chr.getEventInstance() != null || chr.getMap() == null
                || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.changeChannel(slea.readByte() + 1);
    }
}
