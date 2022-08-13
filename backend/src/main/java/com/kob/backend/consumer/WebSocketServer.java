package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {

    public static final ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>(); // 维护连接
    private static final CopyOnWriteArraySet<User> matchpool = new CopyOnWriteArraySet<>(); // 匹配池
    private User user;
    private Session session = null;
    private Game game = null;
    private static UserMapper userMapper;
    @Autowired
    public void setUserMapper(UserMapper userMapper) { // 将UserMapper特殊注入
        WebSocketServer.userMapper = userMapper;
    }
    public static RecordMapper recordMapper;
    @Autowired
    public void setRecordMapper(RecordMapper recordMapper) { // RecordMapper特殊注入
        WebSocketServer.recordMapper = recordMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException { // 建立连接
        this.session = session;
        System.out.println("connected!");

        // 在 token 里解析 userid
        Integer userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if (this.user != null) { // 如果解析成功
            users.put(userId, this);
        }
        else {
            this.session.close();
        }

        System.out.println(users);
    }

    @OnClose
    public void onClose() { // 关闭链接
        System.out.println("disconnected!");

        // 关闭连接同时将该连接从 users 里删掉
        if (this.user != null) {
            users.remove(this.user.getId());
            matchpool.remove(this.user);
        }
    }

    private void startMatching() {
        System.out.println("start matching!");

        matchpool.add(this.user);

        // 调试匹配
        while (matchpool.size() >= 2) {
            Iterator<User> it = matchpool.iterator();
            User a = it.next(), b = it.next();
            matchpool.remove(a);
            matchpool.remove(b);

            Game game = new Game(13, 14, 20, a.getId(), b.getId()); // 进行游戏对局
            game.createMap(); // 生成地图
            users.get(a.getId()).game = game;
            users.get(b.getId()).game = game;

            game.start(); // 开一个线程进行当前对局的等待输入操作

            JSONObject respGame = new JSONObject(); // 将地图相关的信息封装成json
            respGame.put("a_id", game.getPlayerA().getId()); // playerA的相关信息
            respGame.put("a_sx", game.getPlayerA().getSx());
            respGame.put("a_sy", game.getPlayerA().getSy());
            respGame.put("b_id", game.getPlayerB().getId()); // playerB的相关信息
            respGame.put("b_sx", game.getPlayerB().getSx());
            respGame.put("b_sy", game.getPlayerB().getSy());
            respGame.put("map", game.getG()); // 地图信息

            JSONObject respA = new JSONObject();
            respA.put("event", "start-matching");
            respA.put("opponent_username", b.getUsername());
            respA.put("opponent_photo", b.getPhoto());
            respA.put("game", respGame);
            users.get(a.getId()).sendMessage(respA.toJSONString());

            JSONObject respB = new JSONObject();
            respB.put("event", "start-matching");
            respB.put("opponent_username", a.getUsername());
            respB.put("opponent_photo", a.getPhoto());
            respB.put("game", respGame);
            users.get(b.getId()).sendMessage(respB.toJSONString());
        }
    }

    private void stopMatching() {
        System.out.println("stop matching!");

        matchpool.remove(this.user);
    }

    private void move(int direction) { // 蛇的移动

        // 判断当前连接的用户操控的是哪条蛇
        if (game.getPlayerA().getId().equals(user.getId())) {
            game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(user.getId())) {
            game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) { // 从Client接收消息，当作路由
        System.out.println("receive message!");

        // 取出前端发送给后端的消息
        JSONObject data = JSONObject.parseObject(message);
        String event = data.getString("event");
        if ("start-matching".equals(event)) {
            startMatching();
        } else if ("stop-matching".equals(event)) {
            stopMatching();
        } else if ("move".equals(event)) { // 接收到前端传来的蛇的移动的消息
            move(data.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) { // 从后端向当前连接发送信息
        synchronized (this.session) { // 异步通信过程，需要加一个锁
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}