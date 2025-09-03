package com.bruceliu.controller;

import com.bruceliu.bean.User;
import com.bruceliu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author bruceliu
 * @create 2019-10-15 15:01
 * @description
 */
@RestController
@Scope("prototype")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping(value ="/all")
    public List<User> all(){
        return  userService.queryUsers();
    }

    @GetMapping(value ="/queryById/{id}")
    public User getById(@PathVariable("id") Integer id){
        return userService.queryById(id);
    }

    @GetMapping(value ="/deleteById/{id}")
    public Long deleteById(@PathVariable("id") Integer id){
        userService.deleteById(id);
        return 1L;
    }
}
