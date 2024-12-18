package com.swiftsprinttech.wms01.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * @author jiawe
 */
@Data
public class DeliveryInfoVO {
    private String deliveryId;

    // 订单ID
    private String orderId;

    // 交收方式
    private String deliveryMethod;

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
