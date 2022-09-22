package com.example.mdp_android;

public interface IAppendMessages {
    public void handleMessage(String sender, String content);
    public void toggleLock();
}
