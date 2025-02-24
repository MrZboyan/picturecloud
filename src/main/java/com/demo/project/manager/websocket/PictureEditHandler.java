package com.demo.project.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.demo.project.manager.websocket.disruptor.PictureEditEventProducer;
import com.demo.project.manager.websocket.model.PictureEditActionEnum;
import com.demo.project.manager.websocket.model.PictureEditMessageTypeEnum;
import com.demo.project.manager.websocket.model.PictureEditRequestMessage;
import com.demo.project.manager.websocket.model.PictureEditResponseMessage;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.application.service.UserApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有连接的会话，key: pictureId, value: 用户会话集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 建立连接时，保存会话到集合中
     *
     * @param session 用户会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
        // 广播给同一张图片的用户
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 接收到消息时，处理消息
     *
     * @param session 用户会话
     * @param message 操作消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    /**
     * 连接关闭时，处理消息
     *
     * @param session 用户会话
     * @param status 状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        // 响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 全部广播
     *
     * @param pictureId 图片 ID
     * @param pictureEditResponseMessage 图片编辑响应信息
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        this.broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 广播给编辑同一张图片的用户
     *
     * @param pictureId 图片 ID
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 处理进入编辑消息
     * @param pictureEditRequestMessage 编辑请求消息
     * @param session 会话
     * @param user 用户
     * @param pictureId 图片 ID
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                       User user, Long pictureId) throws Exception {
        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑操作消息
     *
     * @param pictureEditRequestMessage 编辑请求消息
     * @param session 会话
     * @param user 用户
     * @param pictureId 图片 id
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            this.broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理退出编辑消息
     *
     * @param pictureEditRequestMessage 编辑请求消息
     * @param session 会话
     * @param user 用户
     * @param pictureId 图片 id
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

}
