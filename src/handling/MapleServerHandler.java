package handling;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.CashShopOperation;
import handling.cashshop.handler.MTSOperation;
import handling.channel.ChannelServer;
import handling.channel.handler.AllianceHandler;
import handling.channel.handler.BBSHandler;
import handling.channel.handler.BeanGame;
import handling.channel.handler.BuddyListHandler;
import handling.channel.handler.ChatHandler;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.FamilyHandler;
import handling.channel.handler.GuildHandler;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.InterServerHandler;
import handling.channel.handler.InventoryHandler;
import handling.channel.handler.ItemMakerHandler;
import handling.channel.handler.MobHandler;
import handling.channel.handler.MonsterCarnivalHandler;
import handling.channel.handler.NPCHandler;
import handling.channel.handler.PartyHandler;
import handling.channel.handler.PetHandler;
import handling.channel.handler.PlayerHandler;
import handling.channel.handler.PlayerInteractionHandler;
import handling.channel.handler.PlayersHandler;
import handling.channel.handler.StatsHandling;
import handling.channel.handler.SummonHandler;
import handling.channel.handler.UserInterfaceHandler;
import handling.login.LoginServer;
import handling.login.handler.CharLoginHandler;
import handling.login.handler.PacketErrorHandler;
import handling.channel.handler.FamilyBuffHandler;
import handling.mina.MaplePacketDecoder;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import server.MTSStorage;
import server.Randomizer;
import server.ServerProperties;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.Pair;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.LoginPacket;

public class MapleServerHandler extends IoHandlerAdapter implements MapleServerHandlerMBean {
    private static boolean Log_Packets;
    private static String nl;
    private static File loggedIPs;
    private static HashMap<String, FileWriter> logIPMap;
    private static boolean debugMode;
    private static EnumSet<RecvPacketOpcode> blocked;
    private static int Log_Size;
    private static ArrayList<LoggedPacket> Packet_Log;
    private static ReentrantReadWriteLock Packet_Log_Lock;
    private static File Packet_Log_Output;
    private int channel;
    private boolean cs;
    private List<String> BlockedIP;
    private Map<String, Pair<Long, Byte>> tracker;

    public static void reloadLoggedIPs() {
        for (final FileWriter fw : MapleServerHandler.logIPMap.values()) {
            if (fw != null) {
                try {
                    fw.write("=== Closing Log ===");
                    fw.write(MapleServerHandler.nl);
                    fw.flush();
                    fw.close();
                } catch (IOException ex) {
                    // 静默处理，不输出错误信息
                }
            }
        }
        MapleServerHandler.logIPMap.clear();

        // 确保目录存在
        if (MapleServerHandler.loggedIPs.getParentFile() != null) {
            MapleServerHandler.loggedIPs.getParentFile().mkdirs();
        }

        // 如果文件不存在，创建空文件
        if (!MapleServerHandler.loggedIPs.exists()) {
            try {
                MapleServerHandler.loggedIPs.createNewFile();
            } catch (IOException e) {
                // 静默处理，不输出错误信息
            }
        }

        // 如果文件存在且是文件，则读取IP列表
        if (MapleServerHandler.loggedIPs.exists() && MapleServerHandler.loggedIPs.isFile()) {
            Scanner sc = null;
            try {
                sc = new Scanner(MapleServerHandler.loggedIPs);
                while (sc.hasNextLine()) {
                    final String line = sc.nextLine().trim();
                    if (line.length() > 0) {
                        try {
                            final FileWriter fw2 = new FileWriter(new File("PacketLog_" + line + ".txt"), true);
                            fw2.write("=== Creating Log ===");
                            fw2.write(MapleServerHandler.nl);
                            fw2.flush();
                            MapleServerHandler.logIPMap.put(line, fw2);
                        } catch (IOException e) {
                            // 静默处理，不输出错误信息
                        }
                    }
                }
            } catch (IOException e) {
                // 静默处理，不输出错误信息
            } finally {
                if (sc != null) {
                    sc.close();
                }
            }
        }
    }

    private static FileWriter isLoggedIP(final IoSession sess) {
        final String a = sess.getRemoteAddress().toString();
        final String realIP = a.substring(a.indexOf(47) + 1, a.indexOf(58));
        return MapleServerHandler.logIPMap.get(realIP);
    }

    public static void log(final SeekableLittleEndianAccessor packet, final RecvPacketOpcode op, final MapleClient c,
            final IoSession io) {
        if (MapleServerHandler.blocked.contains(op)) {
            return;
        }
        try {
            MapleServerHandler.Packet_Log_Lock.writeLock().lock();
            LoggedPacket logged = null;
            if (MapleServerHandler.Packet_Log.size() == MapleServerHandler.Log_Size) {
                logged = MapleServerHandler.Packet_Log.remove(0);
            }
            if (logged == null) {
                logged = new LoggedPacket(packet, op, io.getRemoteAddress().toString(), (c == null) ? -1 : c.getAccID(),
                        (c == null || c.getAccountName() == null) ? "[Null]" : c.getAccountName(),
                        (c == null || c.getPlayer() == null || c.getPlayer().getName() == null) ? "[Null]"
                                : c.getPlayer().getName());
            } else {
                logged.setInfo(packet, op, io.getRemoteAddress().toString(), (c == null) ? -1 : c.getAccID(),
                        (c == null || c.getAccountName() == null) ? "[Null]" : c.getAccountName(),
                        (c == null || c.getPlayer() == null || c.getPlayer().getName() == null) ? "[Null]"
                                : c.getPlayer().getName());
            }
            MapleServerHandler.Packet_Log.add(logged);
        } finally {
            MapleServerHandler.Packet_Log_Lock.writeLock().unlock();
        }
    }

    public static void registerMBean() {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            final MapleServerHandler mbean = new MapleServerHandler();
            mBeanServer.registerMBean(mbean, new ObjectName("handling:type=MapleServerHandler"));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | MalformedObjectNameException
                | NotCompliantMBeanException e) {
            System.out.println("Error registering PacketLog MBean");
            e.printStackTrace();
        }
    }

    public MapleServerHandler() {
        this.channel = -1;
        this.BlockedIP = new ArrayList<String>();
        this.tracker = new ConcurrentHashMap<String, Pair<Long, Byte>>();
    }

    public MapleServerHandler(final int channel, final boolean cs) {
        this.channel = -1;
        this.BlockedIP = new ArrayList<String>();
        this.tracker = new ConcurrentHashMap<String, Pair<Long, Byte>>();
        this.channel = channel;
        this.cs = cs;
    }

    public void writeLog() {
        try {
            final FileWriter fw = new FileWriter(MapleServerHandler.Packet_Log_Output, true);
            try {
                MapleServerHandler.Packet_Log_Lock.readLock().lock();
                final String nl = System.getProperty("line.separator");
                for (final LoggedPacket loggedPacket : MapleServerHandler.Packet_Log) {
                    fw.write(loggedPacket.toString());
                    fw.write(nl);
                }
                fw.flush();
                fw.close();
            } finally {
                MapleServerHandler.Packet_Log_Lock.readLock().unlock();
            }
        } catch (IOException ex) {
            System.out.println("Error writing log to file.");
        }
    }

    public void messageSent(final IoSession session, final Object message) throws Exception {
        final Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    public void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
    }

    public void sessionOpened(final IoSession session) throws Exception {
        final String address = session.getRemoteAddress().toString().split(":")[0];
        final Pair<Long, Byte> track = this.tracker.get(address);
        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;
            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000L) {
                ++count;
            } else if (difference > 20000L) {
                count = 1;
            }
            if (count >= 10) {
                System.out.print("自动断开连接A2");
                this.BlockedIP.add(address);
                this.tracker.remove(address);
                session.close(true);
                return;
            }
        }
        this.tracker.put(address, new Pair<Long, Byte>(System.currentTimeMillis(), count));
        if (this.channel > -1) {
            if (ChannelServer.getInstance(this.channel).isShutdown()) {
                System.out.print("频道服务器尚未开启,发现连接进入，该连接被断开");
                session.close(true);
                return;
            }
        } else if (this.cs) {
            if (CashShopServer.isShutdown()) {
                System.out.print("商城服务器尚未开启,发现连接进入，该连接被断开");
                session.close(true);
                return;
            }
        } else if (LoginServer.isShutdown()) {
            System.out.print("登录服务器尚未开启,发现连接进入，该连接被断开");
            session.close(true);
            return;
        }
        final byte[] serverRecv = { 70, 114, 122, (byte) Randomizer.nextInt(255) };
        final byte[] serverSend = { 82, 48, 120, (byte) Randomizer.nextInt(255) };
        final byte[] ivRecv = ServerConstants.Use_Fixed_IV ? new byte[] { 9, 0, 5, 95 } : serverRecv;
        final byte[] ivSend = ServerConstants.Use_Fixed_IV ? new byte[] { 1, 95, 4, 63 } : serverSend;
        final MapleClient client = new MapleClient(
                new MapleAESOFB(ivSend, (short) (65535 - ServerConstants.MAPLE_VERSION)),
                new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION), session);
        client.setChannel(this.channel);
        final MaplePacketDecoder.DecoderState decoderState = new MaplePacketDecoder.DecoderState();
        session.setAttribute(MaplePacketDecoder.DECODER_STATE_KEY, decoderState);
        session.write(
                LoginPacket.getHello(ServerConstants.MAPLE_VERSION, ServerConstants.Use_Fixed_IV ? serverSend : ivSend,
                        ServerConstants.Use_Fixed_IV ? serverRecv : ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.setAttribute(IdleStatus.READER_IDLE, 60);
        session.setAttribute(IdleStatus.WRITER_IDLE, 60);
        final StringBuilder sb = new StringBuilder();
        if (this.channel > -1) {
            sb.append("[频道服务器] 频道 ").append(this.channel).append(" : ");
        } else if (this.cs) {
            sb.append("[商城服务器]");
        } else {
            sb.append("[登录服务器]");
        }
        sb.append("IoSession opened ").append(address);
        System.out.println(sb.toString());
        World.Client.addClient(client);
        final FileWriter fw = isLoggedIP(session);
        if (fw != null) {
            if (this.channel > -1) {
                fw.write("=== Logged Into Channel " + this.channel + " ===");
                fw.write(MapleServerHandler.nl);
            } else if (this.cs) {
                fw.write("=== Logged Into CashShop Server ===");
                fw.write(MapleServerHandler.nl);
            } else {
                fw.write("=== Logged Into Login Server ===");
                fw.write(MapleServerHandler.nl);
            }
            fw.flush();
        }
    }

    public void sessionClosed(final IoSession session) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            try {
                final FileWriter fw = isLoggedIP(session);
                if (fw != null) {
                    fw.write("=== Session Closed ===");
                    fw.write(MapleServerHandler.nl);
                    fw.flush();
                }
                client.disconnect(true, this.cs);
            } finally {
                World.Client.removeClient(client);
                session.close(true);
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    public void messageReceived(final IoSession session, final Object message) {
        try {
            final SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(
                    new ByteArrayByteStream((byte[]) message));
            if (slea.available() < 2L) {
                return;
            }
            final short header_num = slea.readShort();
            final RecvPacketOpcode[] values = RecvPacketOpcode.values();
            final int length = values.length;
            int i = 0;
            while (i < length) {
                final RecvPacketOpcode recv = values[i];
                if (recv.getValue() == header_num) {
                    if (MapleServerHandler.debugMode && !RecvPacketOpcode.isSpamHeader(recv)) {
                        final StringBuilder sb = new StringBuilder("Received data 已處理 :" + String.valueOf(recv) + "\n");
                        sb.append(HexTool.toString((byte[]) message)).append("\n")
                                .append(HexTool.toStringFromAscii((byte[]) message));
                        System.out.println(sb.toString());
                    }
                    final MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
                    if (!c.isReceiving()) {
                        return;
                    }
                    if (recv.NeedsChecking() && !c.isLoggedIn()) {
                        return;
                    }
                    if (c.getPlayer() == null || !c.isMonitored() || !MapleServerHandler.blocked.contains(recv)) {
                    }
                    if (MapleServerHandler.Log_Packets) {
                        log(slea, recv, c, session);
                    }
                    // 根据角色名保存客户端封包（需要配置启用）
                    final long packetReceiveTime = System.currentTimeMillis();
                    boolean packetHandled = false;
                    Exception packetException = null;
                    // 在 handlePacket 之前保存 player 引用，因为 handlePacket 可能会清空 player
                    final MapleCharacter playerBefore = (c != null) ? c.getPlayer() : null;
                    final String playerNameBefore = (playerBefore != null) ? playerBefore.getName() : null;

                    if (ServerConstants.EnablePlayerPacketLog && c != null && playerBefore != null
                            && playerNameBefore != null) {
                        try {
                            handlePacket(recv, slea, c, this.cs);
                            packetHandled = true;
                        } catch (Exception e) {
                            packetHandled = false;
                            packetException = e;
                        }
                        // handlePacket 后再次检查 player，因为可能被清空
                        final MapleCharacter playerAfter = c.getPlayer();
                        if (playerAfter == null) {
                            // 如果 player 被清空，使用之前保存的信息
                            System.out.println("警告：handlePacket 后 player 被清空 - 角色: " + playerNameBefore);
                        }
                        final String playerName = (playerAfter != null) ? playerAfter.getName() : playerNameBefore;
                        final long packetProcessTime = System.currentTimeMillis() - packetReceiveTime;
                        final String time = FileoutputUtil.CurrentReadable_Time();
                        final String headerHex = String.format("0x%04X", header_num & 0xFFFF);
                        final String packetHex = HexTool.toString((byte[]) message);
                        final String packetAscii = HexTool.toStringFromAscii((byte[]) message);
                        final MapleCharacter player = (playerAfter != null) ? playerAfter : playerBefore;
                        final StringBuilder logMsg = new StringBuilder();
                        logMsg.append("========== 客户端封包记录 ==========\r\n");
                        logMsg.append("时间：").append(time).append(" (时间戳: ").append(packetReceiveTime).append(")\r\n");
                        logMsg.append("线程：").append(Thread.currentThread().getName()).append(" (ID: ")
                                .append(Thread.currentThread().getId()).append(")\r\n");
                        logMsg.append("-----------------------------------\r\n");
                        logMsg.append("【账号信息】\r\n");
                        logMsg.append("  账号名：").append(c.getAccountName() != null ? c.getAccountName() : "null")
                                .append("\r\n");
                        logMsg.append("  账号ID：").append(c.getAccID()).append("\r\n");
                        logMsg.append("  登录状态：").append(c.isLoggedIn() ? "已登录" : "未登录").append("\r\n");
                        logMsg.append("  世界：").append(c.getWorld()).append("\r\n");
                        logMsg.append("  频道：").append(c.getChannel()).append("\r\n");
                        logMsg.append("  IP地址：").append(c.getTempIP() != null && !c.getTempIP().isEmpty()
                                ? c.getTempIP()
                                : (c.getSessionIPAddress() != null ? c.getSessionIPAddress() : "未知")).append("\r\n");
                        logMsg.append("  MAC地址：").append(c.getMac() != null ? c.getMac() : "未知").append("\r\n");
                        logMsg.append("  延迟：").append(c.getLatency()).append("ms\r\n");
                        logMsg.append("-----------------------------------\r\n");
                        logMsg.append("【角色信息】\r\n");
                        // 添加 null 检查，防止 NullPointerException
                        if (player != null) {
                            logMsg.append("  角色名：").append(player.getName()).append("\r\n");
                            logMsg.append("  角色ID：").append(player.getId()).append("\r\n");
                            logMsg.append("  等级：").append(player.getLevel()).append("\r\n");
                            logMsg.append("  职业：").append(player.getJob()).append("\r\n");
                            logMsg.append("  地图ID：").append(player.getMapId()).append("\r\n");
                            logMsg.append("  经验值：").append(player.getExp()).append("\r\n");
                            logMsg.append("  金币：").append(player.getMeso()).append("\r\n");
                        } else {
                            // player 为 null 的情况（理论上不应该发生，但为了安全起见）
                            logMsg.append("  角色名：").append(playerNameBefore != null ? playerNameBefore : "未知")
                                    .append("\r\n");
                            logMsg.append("  角色ID：未知\r\n");
                            logMsg.append("  等级：未知\r\n");
                            logMsg.append("  职业：未知\r\n");
                            logMsg.append("  地图ID：未知\r\n");
                            logMsg.append("  经验值：未知\r\n");
                            logMsg.append("  金币：未知\r\n");
                            System.err.println("警告：player 为 null - playerNameBefore: " + playerNameBefore
                                    + ", playerAfter: " + (playerAfter != null ? playerAfter.getName() : "null"));
                        }
                        logMsg.append("-----------------------------------\r\n");
                        logMsg.append("【封包信息】\r\n");
                        logMsg.append("  操作码：").append(recv.toString()).append("\r\n");
                        logMsg.append("  封包头：").append(headerHex).append(" (").append(header_num).append(")\r\n");
                        logMsg.append("  封包长度：").append(((byte[]) message).length).append(" 字节\r\n");
                        logMsg.append("  处理状态：").append(packetHandled ? "成功" : "失败").append("\r\n");
                        logMsg.append("  处理耗时：").append(packetProcessTime).append("ms\r\n");
                        if (packetException != null) {
                            logMsg.append("  异常信息：").append(packetException.getClass().getName()).append("\r\n");
                            logMsg.append("  异常消息：").append(packetException.getMessage()).append("\r\n");
                            final StackTraceElement[] stackTrace = packetException.getStackTrace();
                            if (stackTrace != null && stackTrace.length > 0) {
                                logMsg.append("  异常堆栈：\r\n");
                                for (int j = 0; j < Math.min(stackTrace.length, 5); j++) {
                                    logMsg.append("    ").append(stackTrace[j].toString()).append("\r\n");
                                }
                            }
                        }
                        logMsg.append("-----------------------------------\r\n");
                        logMsg.append("【封包数据】\r\n");
                        logMsg.append("  十六进制：").append(packetHex).append("\r\n");
                        logMsg.append("  ASCII数据：").append(packetAscii).append("\r\n");
                        logMsg.append("===================================\r\n\r\n");
                        // 确保 playerName 不为 null
                        final String logFileName = (playerName != null && !playerName.isEmpty()) ? playerName : "未知角色";
                        FileoutputUtil.packetLog("logs/客户端封包/" + logFileName + ".log", logMsg.toString());
                        // 如果处理失败，重新抛出异常
                        if (!packetHandled && packetException != null) {
                            throw packetException;
                        }
                    } else {
                        handlePacket(recv, slea, c, this.cs);
                    }
                    final FileWriter fw = isLoggedIP(session);
                    if (fw != null && !MapleServerHandler.blocked.contains(recv)) {
                        if (recv == RecvPacketOpcode.PLAYER_LOGGEDIN && c != null) {
                            fw.write(">> [AccountName: " + ((c.getAccountName() == null) ? "null" : c.getAccountName())
                                    + "] | [IGN: "
                                    + ((c.getPlayer() == null || c.getPlayer().getName() == null) ? "null"
                                            : c.getPlayer().getName())
                                    + "] | [Time: " + FileoutputUtil.CurrentReadable_Time() + "]");
                            fw.write(MapleServerHandler.nl);
                        }
                        fw.write("[" + recv.toString() + "]" + slea.toString(true));
                        fw.write(MapleServerHandler.nl);
                        fw.flush();
                    }
                    return;
                } else {
                    ++i;
                }
            }
            // 收到未定义的封包，记录日志但不断开连接
            final MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
            String playerInfo = "";
            if (c != null && c.getPlayer() != null) {
                playerInfo = "时间：" + FileoutputUtil.CurrentReadable_Time() +
                        " || 玩家名字：" + c.getPlayer().getName() +
                        " || 玩家地图：" + c.getPlayer().getMapId() +
                        " || 玩家等级：" + c.getPlayer().getLevel() +
                        "\r\n";
            } else if (c != null && c.getAccountName() != null) {
                playerInfo = "时间：" + FileoutputUtil.CurrentReadable_Time() +
                        " || 账号：" + c.getAccountName() +
                        "\r\n";
            } else {
                playerInfo = "时间：" + FileoutputUtil.CurrentReadable_Time() + "\r\n";
            }

            final String headerHex = String.format("0x%02X", header_num & 0xFF);
            final String logMsg = playerInfo +
                    "未定义封包头：" + headerHex + " (" + header_num + ")\r\n" +
                    "封包数据：" + HexTool.toString((byte[]) message) + "\r\n" +
                    "ASCII数据：" + HexTool.toStringFromAscii((byte[]) message) + "\r\n\r\n";

            // 记录到日志文件
            FileoutputUtil.packetLog("logs/未定义封包.log", logMsg);

            if (MapleServerHandler.debugMode) {
                final StringBuilder sb2 = new StringBuilder("Received data 未處理 : ");
                sb2.append(HexTool.toString((byte[]) message)).append("\n")
                        .append(HexTool.toStringFromAscii((byte[]) message));
                System.out.println(sb2.toString());
            }

            // 检查是否为可能导致掉线的封包
            // 38错误通常与未知的操作码有关，我们记录详细信息但不断开连接
            if (c != null && c.getPlayer() != null) {
                // 记录38掉线日志
                final String dropLog = "时间：" + FileoutputUtil.CurrentReadable_Time() + 
                        " || 玩家名字：" + c.getPlayer().getName() +
                        "|| 玩家地图：" + c.getPlayer().getMapId() + "\r\n" +
                        "38错误： 暂未定义 ：\r\n" +
                        HexTool.toString((byte[]) message) + "\r\n";
                FileoutputUtil.packetLog("logs/38掉线.log", dropLog);
            }

            // 不处理这个封包，但不断开连接，直接返回
            return;
        } catch (RejectedExecutionException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
        }
    }

    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            client.sendPing();
            super.sessionIdle(session, status);
            return;
        }
        session.close(true);
    }

    public static boolean isSpamHeader(final RecvPacketOpcode header) {
        switch (header) {
            case PONG:
            case NPC_ACTION:
            case MOVE_SUMMON:
            case MOVE_LIFE:
            case MOVE_PLAYER:
            case MOVE_PET:
            case SPECIAL_MOVE:
            case QUEST_ACTION:
            case HEAL_OVER_TIME:
            case STRANGE_DATA:
            case CHANGE_KEYMAP:
            case USE_INNER_PORTAL: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public static void handlePacket(final RecvPacketOpcode header, final SeekableLittleEndianAccessor slea,
            final MapleClient c, final boolean cs) throws Exception {
        switch (header) {
            case PONG: {
                c.pongReceived();
            }
            case PACKET_ERROR: {
                PacketErrorHandler.handlePacket(slea, c);
            }
            case LOGIN_PASSWORD: {
                CharLoginHandler.login(slea, c);
                break;
            }
            case SERVERLIST_REQUEST: {
                CharLoginHandler.ServerListRequest(c);
                break;
            }
            case LICENSE_REQUEST: {
                CharLoginHandler.ServerListRequest(c);
                break;
            }
            case CHARLIST_REQUEST: {
                CharLoginHandler.CharlistRequest(slea, c);
                break;
            }
            case SERVERSTATUS_REQUEST: {
                CharLoginHandler.ServerStatusRequest(c);
                break;
            }
            case CHECK_CHAR_NAME: {
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            }
            case CREATE_CHAR: {
                CharLoginHandler.CreateChar(slea, c);
                break;
            }
            case CHAR_SELECT: {
                CharLoginHandler.Character_WithoutSecondPassword(slea, c);
                break;
            }
            case DELETE_CHAR: {
                final int charId = slea.readInt();
                if (!c.isLoggedIn()) {
                    c.getSession().write(LoginPacket.deleteCharResponse(charId, 1));
                    return;
                }
                c.getSession().write(LoginPacket.deleteCharResponse(charId, c.deleteCharacter(charId)));
                break;
            }
            case SET_GENDER: {
                CharLoginHandler.SetGenderRequest(slea, c);
                break;
            }
            case RSA_KEY: {
                c.getSession().write(LoginPacket.StrangeDATA());
                break;
            }
            case CHANGE_CHANNEL: {
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer());
                break;
            }
            case PLAYER_LOGGEDIN: {
                final int playerid = slea.readInt();
                if (cs) {
                    CashShopOperation.EnterCS(playerid, c);
                    break;
                }
                InterServerHandler.Loggedin(playerid, c);
                break;
            }
            case ENTER_CASH_SHOP: {
                slea.readInt();
                InterServerHandler.EnterCS(c, c.getPlayer());
                break;
            }
            case ENTER_MTS: {
                InterServerHandler.EnterMTS(c, c.getPlayer());
                break;
            }
            case PLAYER_UPDATE: {
                PlayerHandler.UpdateHandler(slea, c, c.getPlayer());
                break;
            }
            case MOVE_PLAYER: {
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            }
            case CHAR_INFO_REQUEST: {
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            }
            case CLOSE_RANGE_ATTACK: {
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), false);
                break;
            }
            case RANGED_ATTACK: {
                PlayerHandler.rangedAttack(slea, c, c.getPlayer());
                break;
            }
            case MAGIC_ATTACK: {
                PlayerHandler.MagicDamage(slea, c, c.getPlayer());
                break;
            }
            case SPECIAL_MOVE: {
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            }
            case PASSIVE_ENERGY: {
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), true);
                break;
            }
            case FACE_EXPRESSION: {
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            }
            case TAKE_DAMAGE: {
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            }
            case HEAL_OVER_TIME: {
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            }
            case CANCEL_BUFF: {
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            }
            case CANCEL_ITEM_EFFECT: {
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            }
            case USE_CHAIR: {
                PlayerHandler.UseChair(slea.readInt(), c, c.getPlayer());
                break;
            }
            case CANCEL_CHAIR: {
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            }
            case USE_ITEMEFFECT: {
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            }
            case SKILL_EFFECT: {
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            }
            case MESO_DROP: {
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            }
            case MONSTER_BOOK_COVER: {
                PlayerHandler.ChangeMonsterBookCover(slea.readInt(), c, c.getPlayer());
                break;
            }
            case CHANGE_KEYMAP: {
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            }
            case CHANGE_MAP: {
                if (cs) {
                    if (ServerConstants.调试输出封包) {
                        System.out.println("退出商城");
                    }
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                    break;
                }
                PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                break;
            }
            case CHANGE_MAP_SPECIAL: {
                PlayerHandler.ChangeMapSpecial(slea, c, c.getPlayer());
                break;
            }
            case USE_INNER_PORTAL: {
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            }
            case TROCK_ADD_MAP: {
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            }
            case LIE_DETECTOR: {
                PlayersHandler.LieDetector(slea, c, c.getPlayer(), false);
                break;
            }
            case LIE_DETECTOR_RESPONSE: {
                PlayersHandler.LieDetectorResponse(slea, c);
                break;
            }
            case LIE_DETECTOR_REFRESH: {
                PlayersHandler.LieDetectorRefresh(slea, c);
                break;
            }
            case ARAN_COMBO: {
                PlayerHandler.AranCombo(c, c.getPlayer());
                break;
            }
            case SKILL_MACRO: {
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            }
            case ITEM_BAOWU: {
                InventoryHandler.UsePenguinBox(slea, c);
                break;
            }
            case ITEM_SUNZI: {
                InventoryHandler.SunziBF(slea, c);
                break;
            }
            case GIVE_FAME: {
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            }
            case TRANSFORM_PLAYER: {
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            }
            case NOTE_ACTION: {
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            }
            case USE_DOOR: {
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            }
            case DAMAGE_REACTOR: {
                PlayersHandler.HitReactor(slea, c);
                break;
            }
            case TOUCH_REACTOR: {
                PlayersHandler.TouchReactor(slea, c);
                break;
            }
            case CLOSE_CHALKBOARD: {
                c.getPlayer().setChalkboard(null);
                break;
            }
            case ITEM_MAKER: {
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            }
            case ITEM_SORT: {
                InventoryHandler.ItemSort(slea, c);
                break;
            }
            case ITEM_GATHER: {
                InventoryHandler.ItemGather(slea, c);
                break;
            }
            case ITEM_MOVE: {
                InventoryHandler.ItemMove(slea, c);
                break;
            }
            case ITEM_PICKUP: {
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            }
            case USE_CASH_ITEM: {
                InventoryHandler.UseCashItem(slea, c);
                break;
            }
            case QUEST_KJ: {
                InventoryHandler.QuestKJ(slea, c, c.getPlayer());
                break;
            }
            case USE_ITEM: {
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            }
            case USE_RETURN_SCROLL: {
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            }
            case USE_UPGRADE_SCROLL: {
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll((byte) slea.readShort(), (byte) slea.readShort(),
                        (byte) slea.readShort(), c, c.getPlayer());
                break;
            }
            case USE_SUMMON_BAG: {
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            }
            case ITEM_MZD: {
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            }
            case USE_SKILL_BOOK: {
                InventoryHandler.UseSkillBook(slea, c, c.getPlayer());
                break;
            }
            case USE_CATCH_ITEM: {
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            }
            case USE_MOUNT_FOOD: {
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            }
            case MOVE_LIFE: {
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            }
            case AUTO_AGGRO: {
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            }
            case FRIENDLY_DAMAGE: {
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            }
            case MONSTER_BOMB: {
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            }
            case NPC_SHOP: {
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            }
            case NPC_TALK: {
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            }
            case NPC_TALK_MORE: {
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            }
            case NPC_ACTION: {
                NPCHandler.NPCAnimation(slea, c);
                break;
            }
            case QUEST_ACTION: {
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            }
            case STORAGE: {
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            }
            case GENERAL_CHAT: {
                ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                break;
            }
            case PARTYCHAT: {
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            }
            case WHISPER: {
                ChatHandler.Whisper_Find(slea, c);
                break;
            }
            case MESSENGER: {
                ChatHandler.Messenger(slea, c);
                break;
            }
            case AUTO_ASSIGN_AP: {
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            }
            case DISTRIBUTE_AP: {
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            }
            case DISTRIBUTE_SP: {
                c.getPlayer().updateTick(slea.readInt());
                StatsHandling.DistributeSP(slea.readInt(), c, c.getPlayer());
                break;
            }
            case PLAYER_INTERACTION: {
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            }
            case GUILD_OPERATION: {
                GuildHandler.Guild(slea, c);
                break;
            }
            case DENY_GUILD_REQUEST: {
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            }
            case ALLIANCE_OPERATION: {
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            }
            case DENY_ALLIANCE_REQUEST: {
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            }
            case BBS_OPERATION: {
                BBSHandler.BBSOperatopn(slea, c);
                break;
            }
            case PARTY_OPERATION: {
                PartyHandler.PartyOperatopn(slea, c);
                break;
            }
            case DENY_PARTY_REQUEST: {
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            }
            case BUDDYLIST_MODIFY: {
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            }
            case CYGNUS_SUMMON: {
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            }
            case SHIP_OBJECT: {
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            }
            case BUY_CS_ITEM: {
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            }
            case TOUCHING_CS: {
                CashShopOperation.TouchingCashShop(c);
                break;
            }
            case COUPON_CODE: {
                FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Coupon : \n" + slea.toString(true));
                System.out.println(slea.toString());
                slea.skip(2);
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                break;
            }
            case CS_UPDATE: {
                CashShopOperation.CSUpdate(c);
                break;
            }
            case TOUCHING_MTS: {
                MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(c.getPlayer().getId()), c);
                break;
            }
            case MTS_TAB: {
                MTSOperation.MTSOperation(slea, c);
                break;
            }
            case DAMAGE_SUMMON: {
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            }
            case MOVE_SUMMON: {
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            }
            case SUMMON_ATTACK: {
                SummonHandler.SummonAttack(slea, c, c.getPlayer());
                break;
            }
            case PET_EXCEPTIONLIST: {
                PetHandler.PickExceptionList(slea, c, c.getPlayer());
                break;
            }
            case SPAWN_PET: {
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            }
            case MOVE_PET: {
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            }
            case PET_CHAT: {
                if (slea.available() < 12L) {
                    break;
                }
                PetHandler.PetChat((int) slea.readLong(), slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            }
            case PET_COMMAND: {
                PetHandler.PetCommand(slea, c, c.getPlayer());
                break;
            }
            case PET_FOOD: {
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            }
            case PET_LOOT: {
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            }
            case PET_AUTO_POT: {
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            }
            case MONSTER_CARNIVAL: {
                MonsterCarnivalHandler.MonsterCarnival(slea, c);
                break;
            }
            case DUEY_ACTION: {
                DueyHandler.DueyOperation(slea, c);
                break;
            }
            case USE_HIRED_MERCHANT: {
                HiredMerchantHandler.UseHiredMerchant(slea, c);
                break;
            }
            case MERCH_ITEM_STORE: {
                HiredMerchantHandler.MerchantItemStore(slea, c);
            }
            case LEFT_KNOCK_BACK: {
                PlayerHandler.leftKnockBack(slea, c);
                break;
            }
            case SNOWBALL: {
                PlayerHandler.snowBall(slea, c);
                break;
            }
            case ChatRoom_SYSTEM: {
                PlayersHandler.ChatRoomHandler(slea, c);
                break;
            }
            case COCONUT: {
                PlayersHandler.hitCoconut(slea, c);
                break;
            }
            case OWL: {
                InventoryHandler.Owl(slea, c);
                break;
            }
            case OWL_WARP: {
                InventoryHandler.OwlWarp(slea, c);
                break;
            }
            case USE_OWL_MINERVA: {
                InventoryHandler.OwlMinerva(slea, c);
                break;
            }
            case RPS_GAME: {
                NPCHandler.RPSGame(slea, c);
                break;
            }
            case UPDATE_QUEST: {
                NPCHandler.UpdateQuest(slea, c);
                break;
            }
            case RING_ACTION: {
                PlayersHandler.RingAction(slea, c);
                break;
            }
            case REQUEST_FAMILY: {
                FamilyHandler.RequestFamily(slea, c);
                break;
            }
            case OPEN_FAMILY: {
                FamilyHandler.OpenFamily(slea, c);
                break;
            }
            case FAMILY_OPERATION: {
                FamilyHandler.FamilyOperation(slea, c);
                break;
            }
            case DELETE_JUNIOR: {
                FamilyHandler.DeleteJunior(slea, c);
                break;
            }
            case DELETE_SENIOR: {
                FamilyHandler.DeleteSenior(slea, c);
                break;
            }
            case USE_FAMILY: {
                FamilyHandler.UseFamily(slea, c);
                break;
            }
            case FAMILY_PRECEPT: {
                FamilyHandler.FamilyPrecept(slea, c);
                break;
            }
            case FAMILY_SUMMON: {
                FamilyHandler.FamilySummon(slea, c);
                break;
            }
            case ACCEPT_FAMILY: {
                FamilyHandler.AcceptFamily(slea, c);
                break;
            }
            case BEANS_GAME1: {
                BeanGame.BeanGame1(slea, c);
                break;
            }
            case BEANS_GAME2: {
                BeanGame.BeanGame2(slea, c);
                break;
            }
            case MOONRABBIT_HP: {
                PlayerHandler.Rabbit(slea, c);
                break;
            }
            case FAMILY_BUFF: {
                FamilyBuffHandler.handleFamilyBuff(slea, c);
                break;
            }
            case EFFECT_ON_OFF:
            case NEW_SX:
            case STRANGE_DATA:
            case UNKNOWN_C1: {
                // 空处理，防止38错误
                // 将未处理的封包码单独记录到logs/目录中
                final String unhandledPacketInfo = "时间: " + FileoutputUtil.CurrentReadable_Time() +
                        " | 封包码: " + header.name() + " (0x" + Integer.toHexString(header.getValue()) + ")" +
                        " | 玩家: " + (c.getPlayer() != null ? c.getPlayer().getName() : "Unknown") +
                        " | 账号: " + (c.getAccountName() != null ? c.getAccountName() : "Unknown") + "\n";
                FileoutputUtil.log(FileoutputUtil.UnhandledPacket_Log, unhandledPacketInfo);
                break;
            }
            default: {
                // 处理未知的操作码，防止38错误导致客户端断开连接
                final String packetInfo = "Unknown Packet Code: " + header.name() + " (0x" + Integer.toHexString(header.getValue()) + ")\n" + slea.toString();
                System.err.println("[" + FileoutputUtil.CurrentReadable_Time() + "] " + packetInfo);
                if (c.getPlayer() != null && c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "未知的操作码: " + header.name() + " (0x" + Integer.toHexString(header.getValue()) + ")");
                }
                FileoutputUtil.log(FileoutputUtil.UnknownPacket_Log, packetInfo);
                break;
            }
        }
    }

    static {
        MapleServerHandler.Log_Packets = true;
        MapleServerHandler.nl = System.getProperty("line.separator");
        MapleServerHandler.loggedIPs = new File("logs/LogIPs.txt");
        MapleServerHandler.logIPMap = new HashMap<String, FileWriter>();
        MapleServerHandler.debugMode = Boolean.parseBoolean(ServerProperties.getProperty("RoyMS.Debug", "false"));
        MapleServerHandler.blocked = EnumSet.noneOf(RecvPacketOpcode.class);
        MapleServerHandler.Log_Size = 10000;
        MapleServerHandler.Packet_Log = new ArrayList<LoggedPacket>(MapleServerHandler.Log_Size);
        MapleServerHandler.Packet_Log_Lock = new ReentrantReadWriteLock();
        MapleServerHandler.Packet_Log_Output = new File("logs/PacketLog.txt");
        reloadLoggedIPs();
        final RecvPacketOpcode[] block = { RecvPacketOpcode.NPC_ACTION, RecvPacketOpcode.MOVE_PLAYER,
                RecvPacketOpcode.MOVE_PET, RecvPacketOpcode.MOVE_SUMMON, RecvPacketOpcode.MOVE_LIFE,
                RecvPacketOpcode.HEAL_OVER_TIME, RecvPacketOpcode.STRANGE_DATA };
        MapleServerHandler.blocked.addAll(Arrays.asList(block));
    }

    private static class LoggedPacket {
        private static final String nl;
        private String ip;
        private String accName;
        private String accId;
        private String chrName;
        private SeekableLittleEndianAccessor packet;
        private long timestamp;
        private RecvPacketOpcode op;

        public LoggedPacket(final SeekableLittleEndianAccessor p, final RecvPacketOpcode op, final String ip,
                final int id, final String accName, final String chrName) {
            this.setInfo(p, op, ip, id, accName, chrName);
        }

        public void setInfo(final SeekableLittleEndianAccessor p, final RecvPacketOpcode op, final String ip,
                final int id, final String accName, final String chrName) {
            this.ip = ip;
            this.op = op;
            this.packet = p;
            this.accName = accName;
            this.chrName = chrName;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("[IP: ").append(this.ip).append("] [").append(this.accId).append('|').append(this.accName)
                    .append('|').append(this.chrName).append("] [Time: ").append(this.timestamp).append(']');
            sb.append(LoggedPacket.nl);
            sb.append("[Op: ").append(this.op.toString()).append(']');
            sb.append(" [Data: ").append(this.packet.toString()).append(']');
            return sb.toString();
        }

        static {
            nl = System.getProperty("line.separator");
        }
    }
}
