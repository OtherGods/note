package com.bruceliu.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author bruceliu
 * @create 2019-10-15 14:39
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    //序列化ID
    private static final long serialVersionUID = -4406399348497545486L;

    private int id;
    private String username;
    private String password;
    private int age;
    private int sex;
    private String birthday;
    private String created;
    private String updated;
    private String note;


    public User(String username, String password, int age, int sex, String birthday, String created, String updated, String note) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.sex = sex;
        this.birthday = birthday;
        this.created = created;
        this.updated = updated;
        this.note = note;
    }
}

