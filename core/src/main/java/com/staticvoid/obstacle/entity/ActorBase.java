package com.staticvoid.obstacle.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Logger;

// holds position size scale rotation and color

// Actors have position at bottom left corner
// as we are using Circle bounds we need to account for this
// in oour updateCollisionShape method, etc.
public abstract class ActorBase extends Actor {

    private static final Logger log =
            new Logger(ActorBase.class.getName(),
                    Logger.DEBUG);

    // == attributes
    private final Circle collisionShape = new Circle();
    private TextureRegion region;

    // == constructors


    public ActorBase() {
        // no args...
    }

    // change collision radius based on size of actor

    // == public methods
    public void setCollisionRadius(float radius) {
        collisionShape.setRadius(radius);
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // no need to call projection matrix or begin/end
        // the Stage will do it for us
        if(region == null) {
            log.error("Region not found on Actor: " + getClass().getName());
            return;
        }

        batch.draw(region,
                getX(), getY(),
                getOriginX(), getOriginY(),
                getWidth(), getHeight(),
                getScaleX(), getScaleY(),
                getRotation()
        );
    }

    public Circle getCollisionShape() {
        return collisionShape;
    }

    @Override
    protected void drawDebugBounds(ShapeRenderer shapeRenderer) {
        if(!getDebug()) {
            log.error("region not set on Actor"); // alternatively, pass region to constructor
            return;
        }

        // draw circle with x in center
        Color oldColor = shapeRenderer.getColor().cpy();
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.x(collisionShape.x, collisionShape.y, 0.1f);
        shapeRenderer.circle(collisionShape.x, collisionShape.y, collisionShape.radius, 30);
        shapeRenderer.setColor(oldColor);
    }

    // whenever the position of Actor changes, positionChanged() is called
//    @Override
//    public void setPosition(float x, float y) {
//
//    }

// instead of overriding setx sety etc etc override protected positionChanged
    @Override
    protected void positionChanged() {
        updateCollisionShape();
    }

    @Override
    protected void sizeChanged() {
        updateCollisionShape();
    }

    // == private methods
    private void updateCollisionShape() {
        float halfWidth = getWidth() / 2f;
        float halfHeight = getHeight() / 2f;

        collisionShape.setPosition(getX() + halfWidth,
                getY() + halfHeight);
    }
}
