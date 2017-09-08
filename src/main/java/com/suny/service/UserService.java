package com.suny.service;

import com.suny.dao.LoginTicketDAO;
import com.suny.dao.UserDAO;
import com.suny.model.LoginTicket;
import com.suny.model.User;
import com.suny.utils.WendaUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Created by 孙建荣 on 17-9-1.上午10:27
 */
@Service
public class UserService {

    private final UserDAO userDAO;

    private final LoginTicketDAO loginTicketDAO;

    @Autowired
    public UserService(UserDAO userDAO, LoginTicketDAO loginTicketDAO) {
        this.userDAO = userDAO;
        this.loginTicketDAO = loginTicketDAO;
    }

    public User selectByName(String name) {
        return userDAO.selectByName(name);
    }

    @Transactional
    public Map<String, Object> register(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(username)) {
            map.put("msg", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("msg", "密码不能为空");
            return map;
        }
        User user = userDAO.selectByName(username);
        if (user != null) {
            map.put("msg", "用户名已经被注册");
            return map;
        }

        // 密码强度
        user = new User();
        user.setName(username);
        user.setSalt(UUID.randomUUID().toString());
        String head = String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000));
        user.setHeadUrl(head);
        user.setPassword(WendaUtil.MD5(password + user.getSalt()));
        userDAO.addUser(user);

        // 登录
        String ticket = addLoginTicket(user.getId());
        map.put("ticket", ticket);
        return map;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        String ticket;
        if (StringUtils.isBlank(username)) {
            map.put("msg", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("msg", "密码不能为空");
            return map;
        }

        User user = userDAO.selectByName(username);
        if (user == null) {
            map.put("msg", "用户名不存在");
            return map;
        }
        if (!WendaUtil.MD5(password + user.getSalt()).equals(user.getPassword())) {
            map.put("msg", "密码不正确");
            return map;
        }
        LoginTicket loginTicket = loginTicketDAO.selectByUserId(user.getId());
        boolean expiredStatus = checkTicketExpired(loginTicket);
        if (!expiredStatus) {
            ticket = addLoginTicket(user.getId());
        } else {
            ticket = loginTicket.getTicket();
        }
        map.put("ticket", ticket);
        map.put("userId", user.getId());
        return map;
    }

    private boolean checkTicketExpired(LoginTicket loginTicket) {
        if (loginTicket == null) {
            return false;
        }
        // 0无效,1有效
        Date expiredDate = loginTicket.getExpired();
        // 时间过期了或者是状态为0就直接删除好了
        if (expiredDate.before(new Date()) || loginTicket.getStatus() == 0) {
            loginTicketDAO.deleteTicket(loginTicket.getId());
            return false;
        }
        return true;
    }

    private String addLoginTicket(int userId) {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(userId);
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * 3600 * 24);
        loginTicket.setExpired(date);
        loginTicket.setStatus(1);
        loginTicket.setTicket(UUID.randomUUID().toString().replaceAll("-", ""));
        loginTicketDAO.addTicket(loginTicket);
        return loginTicket.getTicket();
    }


    public User getUser(int id) {
        return userDAO.selectById(id);
    }

    public void logout(String ticket) {
        loginTicketDAO.updateStatus(ticket, 0);
    }

}







































