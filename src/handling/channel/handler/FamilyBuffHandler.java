package handling.channel.handler;

import client.MapleClient;
import client.MapleCharacter;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.FamilyPacket;
import tools.MaplePacketCreator;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;

public class FamilyBuffHandler {
    
    public static void handleFamilyBuff(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        // 为了防止掉线，我们需要正确处理这个封包
        try {
            // 读取封包数据
            int type = slea.readInt(); // 类型
            
            MapleCharacter player = c.getPlayer();
            if (player == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            
            // 根据类型处理不同的家族Buff操作
            switch (type) {
                case 0: // 可能是请求使用家族Buff
                    if (player.getFamilyId() > 0) {
                        int buffType = slea.readInt();
                        MapleFamilyBuff.MapleFamilyBuffEntry entry = MapleFamilyBuff.getBuffEntry(buffType);
                        if (entry != null && player.canUseFamilyBuff(entry) && player.getCurrentRep() >= entry.rep) {
                            // 应用Buff
                            entry.applyTo(player);
                            player.setCurrentRep(player.getCurrentRep() - entry.rep);
                            player.useFamilyBuff(entry);
                            c.getSession().write(FamilyPacket.changeRep(-entry.rep));
                        }
                    }
                    break;
                    
                case 1: // 可能是其他家族操作
                    // 消耗剩余数据
                    while (slea.available() > 0) {
                        try {
                            slea.readByte();
                        } catch (Exception e) {
                            break;
                        }
                    }
                    break;
                    
                default:
                    // 消耗所有剩余数据以确保数据流正确消耗
                    while (slea.available() > 0) {
                        try {
                            slea.readByte();
                        } catch (Exception e) {
                            break;
                        }
                    }
                    break;
            }
            
            // 发送确认封包以确保客户端状态同步
            c.getSession().write(MaplePacketCreator.enableActions());
        } catch (Exception e) {
            // 即使出现异常，也要确保不导致连接断开
            System.err.println("处理FAMILY_BUFF封包时出现异常: " + e.getMessage());
            e.printStackTrace();
            
            // 发送启用操作封包以确保客户端不会卡住
            try {
                c.getSession().write(MaplePacketCreator.enableActions());
            } catch (Exception ex) {
                // 忽略发送响应时的任何异常
            }
        }
    }
}