package com.kob.backend.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 填充常用函数
@NoArgsConstructor // 填充无参构造函数
@AllArgsConstructor // 填充所有参构造函数
public class User {
    private Integer id;
    private String username;
    private String password;
}
