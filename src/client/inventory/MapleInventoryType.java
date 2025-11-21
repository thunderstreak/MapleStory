package client.inventory;

public enum MapleInventoryType {
    UNDEFINED(0), // 未定义
    EQUIP(1), // 装备
    USE(2), // 消耗
    SETUP(3), // 设置
    ETC(4), // 其他
    CASH(5), // 现金
    EQUIPPED(-1); // 已装备

    byte type;

    MapleInventoryType(int type) {
        this.type = (byte) type;
    }

    public byte getType() {
        return this.type;
    }

    public short getBitfieldEncoding() {
        return (short) (2 << this.type);
    }

    public static MapleInventoryType getByType(byte type) {
        for (MapleInventoryType l : values()) {
            if (l.getType() == type)
                return l;
        }
        return null;
    }

    public static MapleInventoryType getByWZName(String name) {
        switch (name) {
            case "Install":
                return SETUP;
            case "Consume":
                return USE;
            case "Etc":
                return ETC;
            case "Cash":
                return CASH;
            case "Pet":
                return CASH;
        }
        return UNDEFINED;
    }
}
