package com.andrei.vlad;

import com.badlogic.gdx.math.Vector2;

public class Pillar {
    private final Vector2 vector2;
    private boolean isPassed = false;

    public Pillar(Vector2 vector2) {
        this.vector2 = vector2;
    }

    public Vector2 getVector2() {
        return vector2;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }
}
