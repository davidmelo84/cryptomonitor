package com.crypto.controller;

import com.crypto.service.UserActivityTracker;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketController {

    private final UserActivityTracker activityTracker;

    public WebSocketController(UserActivityTracker activityTracker) {
        this.activityTracker = activityTracker;
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload Map<String, Object> payload, Principal principal) {
        String username = principal.getName();
        activityTracker.receiveHeartbeat(username);
    }
}