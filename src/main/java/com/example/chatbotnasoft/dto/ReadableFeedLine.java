package com.example.chatbotnasoft.dto;

import java.util.Map;

/**
 * DTO pour représenter une ligne de FEED lisible
 */
public class ReadableFeedLine {
    
    private String msgType;
    private Map<String, String> champsLisibles;
    
    public ReadableFeedLine() {}
    
    public ReadableFeedLine(String msgType, Map<String, String> champsLisibles) {
        this.msgType = msgType;
        this.champsLisibles = champsLisibles;
    }
    
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public Map<String, String> getChampsLisibles() {
        return champsLisibles;
    }
    
    public void setChampsLisibles(Map<String, String> champsLisibles) {
        this.champsLisibles = champsLisibles;
    }
    
    @Override
    public String toString() {
        return "ReadableFeedLine{" +
                "msgType='" + msgType + '\'' +
                ", champsLisibles=" + champsLisibles +
                '}';
    }
}
