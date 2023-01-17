package com.heima.model.admin.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author 金宗文
 * @version 1.0
 */
@Data
public class AdUserVO {
    private Integer id;
    private String name;
    private String nickname;
    private String image;
    private String email;
    private Date loginTime;
    private Date createdTime;
}