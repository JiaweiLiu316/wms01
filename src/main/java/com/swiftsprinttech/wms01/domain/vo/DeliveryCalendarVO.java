package com.swiftsprinttech.wms01.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeliveryCalendarVO {
    private String deliveryId;

    // 交收方式
    private String deliveryMethod;

    // 姓名
    private String customerName;

    // 联系方式
    private String phoneNumber;


    private List<Map<String,Integer>> products;

    // 交收日期
    private String deliveryDate;

    // 交收地址
    private String deliveryAddress;

    private String deliveryPerson;


    //    备注
    private String description;

    // 是否完成交收
    private Boolean isCompleted;
}
