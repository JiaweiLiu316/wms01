package com.swiftsprinttech.wms01.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swiftsprinttech.wms01.domain.entity.CustomerInfo;
import com.swiftsprinttech.wms01.domain.entity.OrderInfo;
import com.swiftsprinttech.wms01.domain.entity.PaymentInfo;
import com.swiftsprinttech.wms01.domain.vo.OrderInfoVO;
import com.swiftsprinttech.wms01.domain.vo.PaymentDetailVO;
import com.swiftsprinttech.wms01.service.ICustomerInfoService;
import com.swiftsprinttech.wms01.service.IOrderInfoService;
import com.swiftsprinttech.wms01.service.IPaymentInfoService;
import com.swiftsprinttech.wms01.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jiawe
 */
@RestController
@RequestMapping("/api/paymentInfo")
public class PaymentInfoController {
    @Autowired
    private IPaymentInfoService paymentInfoService;
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ICustomerInfoService customerInfoService;

    @GetMapping("/pageList")
    public Result<Page<PaymentDetailVO>> PaymentDetailPage(
            @RequestParam(value = "page") Integer page,
            @RequestParam(value = "size") Integer size,
            @RequestParam(value = "search", required = false) String search
    ){
        Page<PaymentInfo> paymentInfoPage = new Page<>(page, size);
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("payment_date")
                .orderByDesc("is_completed");
        Page<PaymentInfo> page1 = paymentInfoService.page(paymentInfoPage, queryWrapper);
        if (page1.getRecords().isEmpty()){
            return Result.error("暂无数据！");
        }
        List<PaymentDetailVO> collect = page1.getRecords().stream().map(paymentInfo -> {
                    PaymentDetailVO paymentDetailVO = new PaymentDetailVO();
                    BeanUtil.copyProperties(paymentInfo, paymentDetailVO);
                    paymentDetailVO.setPaymentId(paymentInfo.getId());
                    OrderInfo orderInfo = orderInfoService.getById(paymentInfo.getOrderId());
                    paymentDetailVO.setProductsList(orderInfo.getProductIds());
                    paymentDetailVO.setProductsAmount(orderInfo.getPaymentId().split(",").length);
                    CustomerInfo customerInfo = customerInfoService.getById(orderInfo.getCustomerId());
                    paymentDetailVO.setCustomerName(customerInfo.getName());
                    paymentDetailVO.setPhoneNumber(customerInfo.getPhoneNumber());

                    return paymentDetailVO;
                }

        ).collect(Collectors.toList());

        if (search != null && !search.isEmpty()) {
            // 将 search 转为小写，确保不区分大小写
            String searchLower = search.toLowerCase();

            collect = collect.stream()
                    .filter(paymentDetailVO -> {
                        // 对每个字段进行大小写不敏感的比较
                        return (paymentDetailVO.getProductsList() != null && paymentDetailVO.getProductsList().toLowerCase().contains(searchLower))||
                                (paymentDetailVO.getPaymentId() != null && paymentDetailVO.getPaymentId().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getPaymentMethod() != null && paymentDetailVO.getPaymentMethod().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getPaymentDate() != null && paymentDetailVO.getPaymentDate().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getAmountDue() != null && paymentDetailVO.getAmountDue().toString().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getAmountPaid() != null && paymentDetailVO.getAmountPaid().toString().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getCustomerName() != null && paymentDetailVO.getCustomerName().toLowerCase().contains(searchLower)) ||
                                (paymentDetailVO.getPhoneNumber() != null && paymentDetailVO.getPhoneNumber().toLowerCase().contains(searchLower));
                    })
                    .collect(Collectors.toList());
        }

        // 构建分页 VO
        Page<PaymentDetailVO> paymentDetailVOPage = new Page<>();
        paymentDetailVOPage.setRecords(collect);
        paymentDetailVOPage.setCurrent(page1.getCurrent());
        paymentDetailVOPage.setSize(page1.getSize());
        paymentDetailVOPage.setTotal(page1.getTotal());
        return Result.success("查询成功！", paymentDetailVOPage);
    }
    @GetMapping("/{paymentId}")
    public Result<PaymentDetailVO> getPaymentDetailById(@PathVariable String paymentId){
        if (paymentId == null || paymentId.isEmpty()){
            return Result.error("支付ID不能为空！");
        }
        PaymentInfo paymentInfo = paymentInfoService.getById(paymentId);
        if (paymentInfo == null ){
            return Result.error("该支付记录不存在！");
        }

        String orderId = paymentInfo.getOrderId();

        OrderInfo orderInfo = orderInfoService.getById(orderId);

        if (orderInfo == null){
            return Result.error("该订单不存在！");
        }
        String customerId = orderInfo.getCustomerId();
        String productIds = orderInfo.getProductIds();

        CustomerInfo customerInfo = customerInfoService.getById(customerId);

        if (customerInfo == null){
            return Result.error("该支付订单，用户不存在！");
        }

        String customerName = customerInfo.getName();
        String phoneNumber = customerInfo.getPhoneNumber();

        PaymentDetailVO paymentDetailVO = new PaymentDetailVO();

        BeanUtil.copyProperties(paymentInfo,paymentDetailVO);
        paymentDetailVO.setPaymentId(paymentInfo.getId());
        paymentDetailVO.setCustomerName(customerName);
        paymentDetailVO.setPhoneNumber(phoneNumber);
        paymentDetailVO.setProductsList(productIds);
        int size = Arrays.stream(productIds.split(",")).map(String::trim).collect(Collectors.toList()).size();
        paymentDetailVO.setProductsAmount(size);

        return Result.success("查询成功！",paymentDetailVO);

    }
}
