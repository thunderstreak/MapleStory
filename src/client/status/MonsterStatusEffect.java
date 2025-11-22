package client.status;

import server.life.*;
import java.util.concurrent.*;

public class MonsterStatusEffect {
    private MonsterStatus stati;
    private int skill;
    private MobSkill mobskill;
    private boolean monsterSkill;
    private Integer x;
    private ScheduledFuture<?> cancelTask;
    private ScheduledFuture<?> poisonSchedule;
    private boolean reflect;

    public MonsterStatusEffect(final MonsterStatus stat, final Integer x, final int skillId, final MobSkill mobskill,
            final boolean monsterSkill) {
        this.reflect = false;
        this.stati = stat;
        this.skill = skillId;
        this.monsterSkill = monsterSkill;
        this.mobskill = mobskill;
        this.x = x;
    }

    public MonsterStatusEffect(final MonsterStatus stat, final Integer x, final int skillId, final MobSkill mobskill,
            final boolean monsterSkill, final boolean reflect) {
        this.reflect = false;
        this.stati = stat;
        this.skill = skillId;
        this.monsterSkill = monsterSkill;
        this.mobskill = mobskill;
        this.x = x;
        this.reflect = reflect;
    }

    public MonsterStatus getStati() {
        return this.stati;
    }

    public Integer getX() {
        return this.x;
    }

    public void setValue(final MonsterStatus status, final Integer newVal) {
        this.stati = status;
        this.x = newVal;
    }

    public int getSkill() {
        return this.skill;
    }

    public MobSkill getMobSkill() {
        return this.mobskill;
    }

    public boolean isMonsterSkill() {
        return this.monsterSkill;
    }

    public void setCancelTask(final ScheduledFuture<?> cancelTask) {
        this.cancelTask = cancelTask;
    }

    public void setPoisonSchedule(final ScheduledFuture<?> poisonSchedule) {
        this.poisonSchedule = poisonSchedule;
    }

    public void cancelTask() {
        // 添加同步块以防止多线程环境下出现竞态条件
        synchronized (this) {
            if (this.cancelTask != null) {
                try {
                    this.cancelTask.cancel(false);
                } catch (Exception e) {
                    // 输出错误信息以便调试
                    System.err.println("Error cancelling task: " + e.getMessage());
                    e.printStackTrace();
                }
                this.cancelTask = null;
            }
        }
    }

    public void cancelPoisonSchedule() {
        // 添加同步块以防止多线程环境下出现竞态条件
        synchronized (this) {
            if (this.poisonSchedule != null) {
                try {
                    this.poisonSchedule.cancel(false);
                } catch (Exception e) {
                    // 输出错误信息以便调试
                    System.err.println("Error cancelling poison schedule: " + e.getMessage());
                    e.printStackTrace();
                }
                this.poisonSchedule = null;
            }
        }
    }

    public final boolean isReflect() {
        return this.reflect;
    }
}