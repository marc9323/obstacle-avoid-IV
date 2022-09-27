package com.staticvoid.obstacle.entity._old;

import com.staticvoid.obstacle.config.GameConfig;

@Deprecated
public class Player extends GameObjectBase {

    public Player() {
        super(GameConfig.PLAYER_BOUNDS_RADIUS);
        setSize(GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
    }
}
