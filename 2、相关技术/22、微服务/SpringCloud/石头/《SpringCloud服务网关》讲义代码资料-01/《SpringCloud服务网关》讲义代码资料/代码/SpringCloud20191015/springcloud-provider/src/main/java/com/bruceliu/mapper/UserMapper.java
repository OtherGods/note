package com.bruceliu.mapper;

import com.bruceliu.bean.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author bruceliu
 * @create 2019-10-15 14:53
 * @description
 */
@Mapper
public interface UserMapper {


    public User queryById(Integer id);

    public void deleteById(Integer id);

    public List<User> queryUsers();

    public Long addUser(User user);

}
