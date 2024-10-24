package net.samitkumar.messenger.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.samitkumar.messenger.entity.Message;
import net.samitkumar.messenger.entity.User;
import net.samitkumar.messenger.repository.MessageRepository;
import net.samitkumar.messenger.repository.UserRepository;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Objects;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {
    final SimpMessagingTemplate simpMessagingTemplate;
    final MessageRepository messageRepository;
    final UserRepository userRepository;

    @MessageMapping("/public")
    @SendTo("/topic/queue")
    Message publicMessage(@Payload Message message, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        return message;
    }

    @MessageMapping("/private")
    @SendToUser("/queue/private") // send him self
    Message privateMessage(@Payload Message message, Authentication authentication) {
        var user = (User) authentication.getPrincipal();

        Objects.requireNonNull(message.getReceiverId(), "ReceiverId should not be null");

        message.setSenderId(user.getUserId());
        //message to user
        var savedMessage = messageRepository.save(message);
        var messageTo = userRepository.findById(message.getReceiverId()).orElseThrow();
        log.info("private message from {} to {}", user, messageTo);
        simpMessagingTemplate.convertAndSendToUser(messageTo.getUsername(),"/queue/private", savedMessage);
        return savedMessage;
    }

    @MessageMapping("/group")
    Message groupMessage(@Payload Message message, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        Objects.requireNonNull(message.getGroupId(), "GroupId should not be null");

        log.info("private group message from {} to {}", user, message.getGroupId());
        //message to group
        message.setSenderId(user.getUserId());
        var savedMessage  = messageRepository.save(message);
        simpMessagingTemplate.convertAndSend("/topic/"+ message.getGroupId(), savedMessage);
        return savedMessage;
    }

    @MessageExceptionHandler
    public void handleException(RuntimeException runtimeException) {
        log.error(runtimeException.getMessage());
    }
}
