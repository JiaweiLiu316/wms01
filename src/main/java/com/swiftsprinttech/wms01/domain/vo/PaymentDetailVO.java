package com.swiftsprinttech.wms01.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author jiawe
 */
@Data
public class PaymentDetailVO {

    private String paymentId;

    // 订单ID
    private String orderId;

    private String customerName;

    private String phoneNumber;

    private String productsList;
//    商品数量
    private Integer productsAmount;
    /* 付款方式 */
    private String paymentMethod;

    // 付款日期
    private String paymentDate;

    // 应付款额
    private BigDecimal amountDue;

    // 已付款额
    private BigDecimal amountPaid;

    // 是否完成付款
    private Boolean isCompleted;
}
