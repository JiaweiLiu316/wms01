package com.swiftsprinttech.wms01.domain.vo;

import lombok.Data;

@Data
public class DeliveryDetailVO {
    private String deliveryId;

    // 交收方式
    private String deliveryMethod;

    // 姓名
    private String customerName;

    // 联系方式
    private String phoneNumber;


    private String productIds;

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
