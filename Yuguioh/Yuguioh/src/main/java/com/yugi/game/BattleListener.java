// src/main/java/com/yugi/game/BattleListener.java
package com.yugi.game;

public interface BattleListener {
    void onTurn(String playerCard, String aiCard, String winner);
    void onScoreChanged(int playerScore, int aiScore);
    void onDuelEnded(String winner);
    void onError(String message);
}
