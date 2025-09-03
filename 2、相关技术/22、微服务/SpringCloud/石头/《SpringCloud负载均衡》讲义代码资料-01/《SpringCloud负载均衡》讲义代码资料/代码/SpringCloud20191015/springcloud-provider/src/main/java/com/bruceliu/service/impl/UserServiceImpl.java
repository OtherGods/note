package com.bruceliu.service.impl;

import com.bruceliu.bean.User;
import com.bruceliu.mapper.UserMapper;
import com.bruceliu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author bruceliu
 * @create 2019-10-15 14:59
 * @description
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserMapper userMapper;

    @Override
    public User queryById(Integer id) {
        return userMapper.queryById(id);
    }

    @Override
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

    @Override
    public List<User> queryUsers() {
        return userMapper.queryUsers();
    }
}
