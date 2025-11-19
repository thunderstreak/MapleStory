var ���� = "#fEffect/CharacterEff/1114000/2/0#";
var ���� = "#fEffect/CharacterEff/1022223/4/0#";
var ��ɫ��ͷ = "#fUI/UIWindow/Quest/icon6/7#";
var ������ = "#fUI/UIWindow/Quest/icon3/6#";
var ��ɫ��ͷ = "#fUI/UIWindow/Quest/icon2/7#";
var �����Ʒ = "#v1302000#";
var x1 = "1302000,+1"; // ��ƷID,����
var x2;
var x3;
var x4;
var ���� = "#fEffect/CharacterEff/1022223/4/0#";
var �����Ʒ = "#v1302000#";
var add = "#fEffect/CharacterEff/1112903/0/0#"; //������
var aaa = "#fUI/UIWindow.img/Quest/icon9/0#"; //��ɫ�Ҽ�ͷ
var zzz = "#fUI/UIWindow.img/Quest/icon8/0#"; //��ɫ�Ҽ�ͷ
var sss = "#fUI/UIWindow.img/QuestIcon/3/0#"; //ѡ�����
var ������ͷ = "#fUI/Basic/BtHide3/mouseOver/0#";
var ��̾�� = "#fUI/UIWindow/Quest/icon0#";
var ����new = "#fUI/UIWindow/Quest/icon5/1#";
var ��ɫ��ͷ = "#fEffect/CharacterEff/1112908/0/1#"; //�ʹ�3
var ttt1 = "#fEffect/CharacterEff/1062114/1/0#"; //����
var ��ɫ�ǵ� = "#fUI/UIWindow.img/PvP/Scroll/enabled/next2#";
var ca = java.util.Calendar.getInstance();
var year = ca.get(java.util.Calendar.YEAR); //������
var month = ca.get(java.util.Calendar.MONTH) + 1; //����·�
var day = ca.get(java.util.Calendar.DATE); //��ȡ��
var hour = ca.get(java.util.Calendar.HOUR_OF_DAY); //���Сʱ
var minute = ca.get(java.util.Calendar.MINUTE); //��÷���
var second = ca.get(java.util.Calendar.SECOND); //�����
var weekday = ca.get(java.util.Calendar.DAY_OF_WEEK);
var ������ͷ = "#fUI/Basic/BtHide3/mouseOver/0#";
var ���� = "#fEffect/CharacterEff/1022223/4/0#";

var �۰��� = "#fItem/Etc/0427/04270005/Icon8/1#"; //
var �ջ� = "#fUI/PredictHarmony/card/19#"; //��ƬЧ���ջ�
var Ц = "#fUI/GuildBBS/GuildBBS/Emoticon/Basic/0#"; //Ц��
var ���Ҷ = "#fMap/MapHelper/weather/maple/2#";
var ���Ҷ = "#fMap/MapHelper/weather/maple/1#";
var ��Ů = "#fMap/MapHelper/weather/witch/0#"; //��Ů
var ���� = "#fMap/MapHelper/weather/balloon/4#"; //����
var ��� = "#fMap/MapHelper/weather/LoveEffect2/4/0#"; //���
var õ�� = "#fMap/MapHelper/weather/rose/0#"; //õ�廨
var �̻� = "#fMap/MapHelper/weather/squib/squib1/3#"; //�̻�

var ��ۺ찮�� = "#fItem/Etc/0427/04270001/Icon8/4#"; //
var С�ۺ찮�� = "#fItem/Etc/0427/04270001/Icon8/5#"; //
var С���� = "#fItem/Etc/0427/04270001/Icon9/0#"; //
var ����� = "#fItem/Etc/0427/04270001/Icon9/1#"; //
var Сˮ�� = "#fItem/Etc/0427/04270001/Icon10/5#"; //
var ��ˮ�� = "#fItem/Etc/0427/04270001/Icon10/4#"; //
var tz = "#fEffect/CharacterEff/1082565/4/0#"; //������
var tz1 = "#fEffect/CharacterEff/1082565/0/0#"; //������
var tz2 = "#fEffect/CharacterEff/1082565/2/0#"; //������
var а��С�� = "#fEffect/CharacterEff/1112960/3/0#"; //а��С�� ��С��
var а��С��2 = "#fEffect/CharacterEff/1112960/3/1#"; //а��С�� ����
var ���� = "#fEffect/SetEff/208/effect/walk2/4#";
var ����1 = "#fEffect/SetEff/208/effect/walk2/3#";
var С�� = "#fMap/MapHelper/weather/birthday/2#";
var �һ� = "#fMap/MapHelper/weather/rose/4#";
var ����Ҷ = "#fMap/MapHelper/weather/maple/3#";
var С�̻� = "#fMap/MapHelper/weather/squib/squib4/1#";
var ���� = "#fMap/MapHelper/weather/witch/3#";

function start() {
    status = -1;

    action(1, 0, 0);
}

function action(mode, type, selection) {
    
        cm.sendOk("�ڴ�������");
        cm.dispose();
 


    if (mode == -1) {
        cm.dispose();
    } else {
        if (status >= 0 && mode == 0) {

            cm.sendOk("��л��Ĺ��٣�");
            cm.dispose();
            return;
        }
        // if (mode == 1) {
        //     status++;
        // } else {
        //     status--;
        // }
        // if (status == 0) {
        //     var tex2 = "";
        //     var text = "";
        //     for (i = 0; i < 10; i++) {
        //         text += "";
        //     }
        //     //  text += ""+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+����+"\r\n\r\n"
        //     text += "\t  " + ���� + ���� + "#eת��ϵͳ#b��255����ת����" + ���� + ���� + " #k#n\r\n#r\t\t\t\t\r\n\t\t\t\t" + ���� + "��ɫת���ȼ�:#b" + cm.getClient().getPlayer().getBeans() + "��" + ���� + "\r\n\r\n"
        //     text += "\t" + ��ɫ��ͷ + "#L0##bת����ϵ˵����ת����Ƶ����Ч#k#l\r\n"
        //     text += "\t" + ��ɫ��ͷ + "#L1#��ʼת����#r��Ҫ#v4000464##v4310149#��10��#k#l\r\n\r\n" //3
        //     text += "\t" + ��ɫ��ͷ + "#L2#ת��������#r#v1082102#X1(+100ȫ����HP+5000)ת��1��#k#l\r\n\r\n" //3
        //     text += "\t" + ��ɫ��ͷ + "#L3#ת��������#r#v1022048#X1(+200ȫ����HP+5000)ת��3��#k#l\r\n\r\n" //3
        //     text += "\t" + ��ɫ��ͷ + "#L4#ת��������#r#v1072153#X1(+300ȫ����HP+5000)ת��5��#k#l\r\n\r\n" //3
        //     text += "" + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + ���� + "\r\n"

        //     cm.sendSimple(text);

        // } else if (selection == 0) { //��ϵ˵��
        //     cm.sendOk("#e����ת��ϵͳ,����Խ��Խǿ��\r\n#bת��˵�������10�����ֱ����������Ժͼ��̼���\r\n��#rPS��ת���ȼ�+1����#k");

        // } else if (selection == 1) { //ת��
        //     if (cm.getClient().getPlayer().getBeans() > 5) {
        //         cm.sendOk("ת���ȼ����5�������Ѿ���������������ܣ�");
        //         cm.dispose();
        //     } else if (cm.getLevel() < 255) {
        //         cm.sendOk("ת��Ҫ��ȼ��ﵽ255������������û���ʸ�ת��");
        //         cm.dispose();
        //     } else if (cm.getPlayer().getMeso() < 200000000) {
        //         cm.sendOk("��Ҫ2E��Ҳ���ת��.");
        //         cm.dispose();
        //     } else if (!cm.haveItem(4000464, 2)) {
        //         cm.sendOk("��Ҫ�й���X2��");
        //         cm.dispose();
        //     } else {
        //         cm.setPlayerStat("LVL", 11);
        //         cm.changeJob(000)
        //         cm.spawnMonster(9300340, 2);
        //         cm.gainItem(4000464, -10);
        //         cm.gainItem(4310149, -10);
        //         cm.getPlayer().gainBeans(1);
        //         // cm.gainAp(+5);//ת������
        //         cm.sendOk("��ϲ��ת���ɹ����뻻����Ч��.");
        //         cm.����(1, "[ת�����ֲ�]�����" + cm.getPlayer().getName() + "ת���ɹ�����ϲ�ɺأ������������ˣ�");
        //         cm.ȫ��Ư������("��ϲ���[" + cm.getName() + "]ת���ɹ�����ϲ�ɺأ������������ˣ�", 5120009);
        //         cm.dispose();
        //         return;
        //     }

        // } else if (selection == 2) { //ת��
        //     if (cm.getClient().getPlayer().getBeans() < 1) {
        //         cm.sendOk("����ת���ȼ���������ȡ������");
        //         cm.dispose();
        //     } else if (cm.getQuestStatus(1000010) == 0) {
        //         cm.completeQuest(1000010);
        //         cm.gainItem(1082102, 100, 100, 100, 100, 5000, 5000, 100, 100, 0, 0, 0, 0, 0, 0);
        //         // cm.gainAp(+5);//ת������
        //         cm.sendOk("��ϲ��ת��������ȡ�ɹ���.");
        //         cm.����(1, "[ת������]�����" + cm.getPlayer().getName() + "ת��������ȡ�ɹ�����ϲ�ɺأ�");
        //         cm.ȫ��Ư������("��ϲ���[" + cm.getName() + "]ת��������ȡ�ɹ�����ϲ�ɺأ�", 5120008);
        //         cm.dispose();
        //     } else
        //         cm.sendOk("��Ǹ���Ѿ���ȡ���ý���~");
        //     cm.dispose();

        // } else if (selection == 3) { //ת��
        //     if (cm.getClient().getPlayer().getBeans() < 3) {
        //         cm.sendOk("����ת���ȼ���������ȡ������");
        //         cm.dispose();
        //     } else if (cm.getQuestStatus(1000011) == 0) {
        //         cm.completeQuest(1000011);
        //         cm.gainItem(1022048, 200, 200, 200, 200, 5000, 5000, 200, 200, 0, 0, 0, 0, 0, 0);
        //         // cm.gainAp(+5);//ת������
        //         cm.sendOk("��ϲ��ת��������ȡ�ɹ���.");
        //         cm.����(1, "[ת������]�����" + cm.getPlayer().getName() + "ת��������ȡ�ɹ�����ϲ�ɺأ�");
        //         cm.ȫ��Ư������("��ϲ���[" + cm.getName() + "]ת��������ȡ�ɹ�����ϲ�ɺأ�", 5120006);
        //         cm.dispose();
        //     } else
        //         cm.sendOk("��Ǹ���Ѿ���ȡ���ý���~");
        //     cm.dispose();

        // } else if (selection == 4) { //ת��
        //     if (cm.getClient().getPlayer().getBeans() < 5) {
        //         cm.sendOk("����ת���ȼ���������ȡ������");
        //         cm.dispose();
        //     } else if (cm.getQuestStatus(1000012) == 0) {
        //         cm.completeQuest(1000012);
        //         cm.gainItem(1072153, 300, 300, 300, 300, 5000, 5000, 300, 300, 0, 0, 0, 0, 0, 0);
        //         // cm.gainAp(+5);//ת������
        //         cm.sendOk("��ϲ��ת��������ȡ�ɹ���.");
        //         cm.����(1, "[ת������]�����" + cm.getPlayer().getName() + "ת��������ȡ�ɹ�����ϲ�ɺأ�");
        //         cm.ȫ��Ư������("��ϲ���[" + cm.getName() + "]ת��������ȡ�ɹ�����ϲ�ɺأ�", 5120007);
        //         cm.dispose();
        //     } else
        //         cm.sendOk("��Ǹ���Ѿ���ȡ���ý���~");



    }
}
