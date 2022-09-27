package com.staticvoid.obstacle.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.staticvoid.obstacle.config.GameConfig;

public class PlayerActor extends ActorBase {

    // == constructors ==
    public PlayerActor() {
        setCollisionRadius(GameConfig.PLAYER_BOUNDS_RADIUS);
        setSize(GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
    }

    // Stage calls the actor act and draw methods
    // here PlayerActor extends ActorBase which override draw
    // and PlayerActor itself override act().  Stage calls these
    @Override
    public void act(float deltaTime) {
        super.act(deltaTime); // calls all the actions on any Actor instances
        update();
    }

    private void update() {
        // discrete input handling would be optimal
        float xSpeed = 0;

        // polling, recall this is happening inside Game Screen render method
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            xSpeed = GameConfig.MAX_PLAYER_X_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            xSpeed -= GameConfig.MAX_PLAYER_X_SPEED;
        }

        setX(getX() + xSpeed);

        blockPlayerFromLeavingTheWorld();
    }

    private void blockPlayerFromLeavingTheWorld() {
        // this line is equivalent to the following commented out lines
        // feed clamp:  value to clamp, minimum, maximum.
        float playerX = MathUtils.clamp(getX(),
                0,
                GameConfig.WORLD_WIDTH - getWidth());

        setPosition(playerX, getY());
    }
}