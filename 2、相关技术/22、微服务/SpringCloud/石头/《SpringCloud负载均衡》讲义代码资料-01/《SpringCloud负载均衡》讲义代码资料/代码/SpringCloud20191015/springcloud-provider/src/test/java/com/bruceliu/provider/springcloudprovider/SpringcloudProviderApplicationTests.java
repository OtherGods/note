package com.bruceliu.provider.springcloudprovider;

import com.bruceliu.bean.User;
import com.bruceliu.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringcloudProviderApplicationTests {

    @Autowired
    UserService userService;

    @Test
    public void contextLoads() {
        List<User> users = userService.queryUsers();
        for (User user : users) {
            System.out.println(user);
        }
    }

}
