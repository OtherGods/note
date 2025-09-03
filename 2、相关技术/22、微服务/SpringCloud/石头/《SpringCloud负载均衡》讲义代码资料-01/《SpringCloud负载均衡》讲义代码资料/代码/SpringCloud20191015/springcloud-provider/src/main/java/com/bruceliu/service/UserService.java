package com.bruceliu.service;

import com.bruceliu.bean.User;

import java.util.List;

/**
 * @author bruceliu
 * @create 2019-10-15 14:59
 * @description
 */
public interface UserService {


    public User queryById(Integer id);

    public void deleteById(Integer id);

    public List<User> queryUsers();
}
