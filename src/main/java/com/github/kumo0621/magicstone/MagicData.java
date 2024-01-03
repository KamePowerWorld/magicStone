package com.github.kumo0621.magicstone;
public class MagicData {
    private int magicNumber;
    private String magicType;
    private String target;
    private int power;
    private int range;
    private int effect;
    private String message;

    public MagicData(int magicNumber, String magicType, String target, int power, int range, int effect, String message) {
        this.magicNumber = magicNumber;
        this.magicType = magicType;
        this.target = target;
        this.power = power;
        this.range = range;
        this.effect = effect;
        this.message = message;
    }

    // ゲッターメソッド
    public int getMagicNumber() {
        return magicNumber;
    }

    public String getMagicType() {
        return magicType;
    }

    public String getTarget() {
        return target;
    }

    public int getPower() {
        return power;
    }

    public int getRange() {
        return range;
    }

    public int getEffect() {
        return effect;
    }

    public String getMessage() {
        return message;
    }
}