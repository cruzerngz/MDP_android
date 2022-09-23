package com.example.mdp_android;

import android.util.Log;

import java.util.ArrayList;

public class ObstacleManager {
    private ArrayList<Obstacle> obstacles = new ArrayList<>();

    public Obstacle getObstacle(String id){
       for(Obstacle o: obstacles){
           if(id.equals(o.obstacleID)){
               return o;
           }
       }

       return null;
    }

    public void clearObstacles(){
        Log.e("sadfd", "size was " + obstacles.size());
        obstacles.clear();
        Log.e("sadfd", "size is now " + obstacles.size());
    }


    public void addObstacle(Obstacle o) {
        if(!checkExists(o.obstacleID))
            obstacles.add(o);
    }

    public boolean checkExists(String id){
        for(Obstacle o : obstacles){
            if(id.equals(o.obstacleID))
                return true;
        }
        return false;
    }

    public void removeObstacle(String id) {

        Obstacle ref =  null;
        for(Obstacle o : obstacles){
            if(o.obstacleID.equals(id)){
                ref = o;
                break;
            }
        }

        if(ref != null){
            obstacles.remove(ref);
        }

    }

    public void updateObstacle(String obstacleID, String x, String y, String face, String imageID){
        for(Obstacle o : obstacles){
            if(o.obstacleID.equals(obstacleID)){
                o.x = x.equals("") ? o.x : x;
                o.y = y.equals("") ? o.y : y;
                o.face = face.equals("") ? o.face : face;
                o.imageID = x.equals("") ? o.imageID : imageID;
                break;
            }
        }
    }



}
