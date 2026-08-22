package com.ksh.companybackend.config;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketProbeController {

    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public String echo(String message) {
        return message;
    }
}
