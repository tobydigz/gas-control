package com.digzdigital.gascontrol.event;

/**
 * Created by Digz on 07/03/2017.
 */

public  final class MessageEvent {

    public final EventType eventType;

    public MessageEvent(EventType eventType){
        this.eventType = eventType;
    }


}