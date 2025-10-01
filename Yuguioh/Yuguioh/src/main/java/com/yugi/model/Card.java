// src/main/java/com/yugi/model/Card.java
package com.yugi.model;

public class Card {
    private final String name;
    private final int atk;
    private final int def;
    private final String imageUrl;
    private boolean used = false;

    public Card(String name, int atk, int def, String imageUrl) {
        this.name = name;
        this.atk = atk;
        this.def = def;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public String getImageUrl() { return imageUrl; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    @Override
    public String toString() {
        return name + " (ATK:" + atk + " DEF:" + def + ")";
    }
}
