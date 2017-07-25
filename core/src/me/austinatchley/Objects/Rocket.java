package me.austinatchley.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.TimeUtils;
import com.sun.org.apache.bcel.internal.generic.CALOAD;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.bind.util.ValidationEventCollector;

import me.austinatchley.States.GameState;
import me.austinatchley.States.State;

public class Rocket extends SpaceObject {
    private static final int VERTICAL_OFF = 20;
    private static final float CHANGE = 10f * DEG2RAD;

    public ParticleEffect thruster1, thruster2;

    public ArrayList<Missile> shots;
    private long lastShotTime;

    public Rocket(World world){
        super(world);
        image = new Texture("outline.png");
        sprite = new Sprite(image);

        thruster1 = new ParticleEffect();
        thruster2 = new ParticleEffect();
        thruster1.load(Gdx.files.internal("rocket_thruster.p"), Gdx.files.internal(""));
        thruster2.load(Gdx.files.internal("rocket_thruster.p"), Gdx.files.internal(""));

        init();

        shots = new ArrayList<Missile>();

        thruster1.start();
        thruster1.setPosition(body.getPosition().x, body.getPosition().y);
        thruster1.getEmitters().first().getAngle().setLow(-85f);
        thruster1.getEmitters().first().getAngle().setHigh(-95f);

        thruster2.start();
        thruster2.setPosition(body.getPosition().x, body.getPosition().y);
        thruster2.getEmitters().first().getAngle().setLow(-85f);
        thruster2.getEmitters().first().getAngle().setHigh(-95f);
        System.out.println(thruster2.getEmitters().first().getAngle().getLowMin() + "");
        System.out.println(thruster2.getEmitters().first().getAngle().getHighMax() + "");
    }

    public void init() {
        BodyDef rocketBodyDef = new BodyDef();
        rocketBodyDef.type = BodyDef.BodyType.KinematicBody;
        rocketBodyDef.position.set((State.WIDTH - image.getWidth())/ 2, VERTICAL_OFF);

        body = world.createBody(rocketBodyDef);

        MassData rocketMassData = new MassData();
        rocketMassData.mass = 10f;
        body.setMassData(rocketMassData);
        body.setUserData("Rocket");

        //TODO: POLYGONSHAPE AROUND ROCKET
        Vector2 boxSize = GameState.p2m(image.getWidth()/2, image.getHeight());
        PolygonShape rocketShape = new PolygonShape();
        rocketShape.setAsBox(boxSize.x, boxSize.y);

        FixtureDef rocketFixtureDef = new FixtureDef();
        rocketFixtureDef.shape = rocketShape;

        Fixture rocketFixture = body.createFixture(rocketFixtureDef);
        rocketFixture.setUserData("Rocket");
        System.out.println("Rocket width: " + rocketShape.getRadius());
        rocketShape.dispose();
    }

    public void render(SpriteBatch batch){
        Vector2 pos = getPosition();
        float rotation = body.getAngle() / DEG2RAD;

        sprite.setPosition(pos.x - image.getWidth()/2f, pos.y - image.getHeight()/2f);
        sprite.setRotation(rotation);

        if(thruster1.isComplete())
            thruster1.reset();

        if(thruster2.isComplete())
            thruster2.reset();

        thruster1.setPosition(sprite.getX() + sprite.getWidth() * 0.1f, sprite.getY() + sprite.getHeight() * 0.4f);
        thruster2.setPosition(sprite.getX() + sprite.getWidth() * 0.9f, sprite.getY() + sprite.getHeight() * 0.4f);

        thruster1.getEmitters().first().getAngle().setLow(rotation - 90f);
        thruster1.getEmitters().first().getAngle().setHigh(rotation - 90f);

        thruster2.getEmitters().first().getAngle().setLow(rotation - 90f);
        thruster2.getEmitters().first().getAngle().setHigh(rotation - 90f);

        thruster1.update(Gdx.graphics.getDeltaTime());
        thruster1.draw(batch);
        thruster2.update(Gdx.graphics.getDeltaTime());
        thruster2.draw(batch);

        Iterator<Missile> iterator = shots.iterator();
        while(iterator.hasNext()){
            Missile shot = iterator.next();
            shot.render(batch);
            if(shot.isOutOfBounds()){
                shot.dispose();
                iterator.remove();
            }
        }

        // draw it as a normal sprite (on top)
        sprite.draw(batch);
    }

    public void rotateTowards(Vector2 target){
        Vector2 toTarget = new Vector2(target.x - body.getPosition().x,
                target.y - body.getPosition().y);

        float desiredAngle = MathUtils.atan2(-toTarget.x, toTarget.y);
        float totalRotation =  desiredAngle - body.getAngle();
        while ( totalRotation < -180 * DEG2RAD ) totalRotation += 360 * DEG2RAD;
        while ( totalRotation >  180 * DEG2RAD ) totalRotation -= 360 * DEG2RAD;
        float newAngle = body.getAngle() + Math.min(CHANGE, Math.max(-CHANGE, totalRotation));

        setTransform(body.getPosition(), newAngle);
    }

    public void moveTo(Vector2 target) {
        //rotateTowards(target);
        setTransform(target.x + GameState.PPM*image.getWidth()/4f, target.y, body.getAngle());
    }

    public void shootMissile(){
        Missile shot = new Missile(world,
                new Vector2(
                        getPosition().x + image.getWidth() / 2,
                        getPosition().y + image.getHeight()),
                0f,
                200f);
        shot.flip();
        //TODO: center missile. add static field to gsm?

        shots.add(shot);
        lastShotTime = TimeUtils.nanoTime();
    }

    public boolean canShoot(){
        return TimeUtils.nanoTime() - lastShotTime > 500000000;
    }
}
