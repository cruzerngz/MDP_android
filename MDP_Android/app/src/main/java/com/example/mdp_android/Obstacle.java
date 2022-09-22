package com.example.mdp_android;

public class Obstacle {
    public String x;
    public String y;
    public String face;
    public String obstacleID;
    public String imageID;


    public Obstacle(String obstacleID, String x, String y, String face,  String imageID) {
        this.x = x;
        this.y = y;
        this.face = face;
        this.obstacleID = obstacleID;
        this.imageID = imageID;
    }
}
