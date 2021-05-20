package com.lxk.project.dbTest.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author macos·lxk
 * @create 2021/5/12 下午12:36
 */

@Table(name = "user")
@Entity
public class User {

    @Id
    private Integer id;
    private String name;
    private String password;
    private Integer age;
    private Integer deleteFlag;

    public User() {}

    public User(Integer id, String name, String password, Integer age, Integer deleteFlag) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.age = age;
        this.deleteFlag = deleteFlag;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}
