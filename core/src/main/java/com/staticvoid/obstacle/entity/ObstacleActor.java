package com.staticvoid.obstacle.entity;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.utils.Pool;
import com.staticvoid.obstacle.config.GameConfig;

public class ObstacleActor extends ActorBase implements Pool.Poolable {

    // == attributes
    private float ySpeed = GameConfig.MEDIUM_OBSTACLE_SPEED;
    private boolean hit;

    // == constructors ==
    public ObstacleActor() {
        setCollisionRadius(GameConfig.OBSTACLE_BOUNDS_RADIUS);
        setSize(GameConfig.OBSTACLE_SIZE, GameConfig.OBSTACLE_SIZE);
    }

    // == public
    @Override
    public void act(float deltaTime) {
        super.act(deltaTime);
        update();
    }

    public void update() {
        setY(getY() - ySpeed);
    }

    public void setYSpeed(float ySpeed) {
        this.ySpeed = ySpeed;
    }

    public boolean isPlayerColliding(PlayerActor player) {
        Circle playerBounds = player.getCollisionShape();

        boolean overlaps = Intersector.overlaps(playerBounds, getCollisionShape());

        hit = overlaps;

        return overlaps;
    }

    public boolean isNotHit() {
        return !hit;
    }

    @Override
    public void reset() { // resets attributes prior to returning to Pool
        // default state: region null,
        setRegion(null);
        hit = false;
    }
}
