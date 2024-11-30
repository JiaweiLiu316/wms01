package com.swiftsprinttech.wms01.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftsprinttech.wms01.domain.entity.*;
import com.swiftsprinttech.wms01.domain.vo.DeliveryCalendarVO;
import com.swiftsprinttech.wms01.domain.vo.DeliveryDetailVO;
import com.swiftsprinttech.wms01.service.*;
import com.swiftsprinttech.wms01.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    @Autowired
    private IDeliveryInfoService deliveryInfoService;

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private ICustomerInfoService customerInfoService;

    @Autowired
    private IProductInfoService productInfoService;
    @Autowired
    private IOrderItemService orderItemService;

    @GetMapping("/list")
    public Result<List<DeliveryCalendarVO>> getDeliveryList(){
        QueryWrapper<DeliveryInfo> deliveryInfoQueryWrapper = new QueryWrapper<>();
        deliveryInfoQueryWrapper.orderByAsc("delivery_date");
        List<DeliveryInfo> deliveryInfoList = deliveryInfoService.list(deliveryInfoQueryWrapper);
        if (deliveryInfoList == null || deliveryInfoList.isEmpty()){
            return Result.error("暂无数据！");
        }
        List<DeliveryCalendarVO> result = deliveryInfoList.stream().map(deliveryInfo -> {
            DeliveryCalendarVO deliveryCalendarVO = new DeliveryCalendarVO();
            BeanUtil.copyProperties(deliveryInfo, deliveryCalendarVO);
            String orderId = deliveryInfo.getOrderId();
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            String customerId = orderInfo.getCustomerId();
            CustomerInfo customerInfo = customerInfoService.getById(customerId);

            deliveryCalendarVO.setCustomerName(customerInfo.getName());
            deliveryCalendarVO.setPhoneNumber(customerInfo.getPhoneNumber());

            List<String> splitProductIds = Arrays.asList(orderInfo.getProductIds().replace("[", "").replace("]", "").trim().split(","));

            ArrayList<Map<String,Integer>> products = new ArrayList<>();
            if (splitProductIds != null && !splitProductIds.isEmpty()) {
               splitProductIds.forEach(item -> {
                   LambdaQueryWrapper<OrderItem> orderItemLambdaQueryWrapper = new LambdaQueryWrapper<>();
                   LambdaQueryWrapper<OrderItem> orderItemQuery = orderItemLambdaQueryWrapper.eq(OrderItem::getOrderId, orderId).eq(OrderItem::getProductId, item.trim());
                   OrderItem orderItem = orderItemService.getOne(orderItemQuery);
                   Integer quantity = orderItem.getQuantity();
                   LambdaQueryWrapper<ProductInfo> productInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
                   productInfoLambdaQueryWrapper.eq(ProductInfo::getId,item.trim());
                   ProductInfo productInfo = productInfoService.getOne(productInfoLambdaQueryWrapper);
                   if (productInfo != null ){
                       Map<String, Integer> productItem = new HashMap<>();
                       productItem.put(productInfo.getName(),quantity);
                       products.add(productItem);
                   }
                });
            }
            deliveryCalendarVO.setProducts(products);
            return deliveryCalendarVO;
        }).collect(Collectors.toList());
        return Result.success("查询成功",result);
    }

    @GetMapping("/pageList")
    public Result<Page<DeliveryDetailVO>> getAllDelivery(
            @RequestParam(value = "page") Integer page,
            @RequestParam(value = "size") Integer size,
            @RequestParam(value = "search", required = false) String search // 新增 search 参数
    ){
        // 构建分页查询条件
        Page<DeliveryInfo> deliveryInfoPage= new Page<>(page, size);
        QueryWrapper<DeliveryInfo> queryWrapper = new QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            // 对 DeliveryInfo 表的字段进行模糊查询
            queryWrapper.like("delivery_method", search) // 交收方式
                    .or().like("delivery_date", search) // 交收日期
                    .or().like("delivery_address", search) // 交收地址
                    .or().like("delivery_person", search) // 交收人员
                    .or().like("description", search) // 备注
                    .or().like("order_id", search); // 订单ID

        }
        queryWrapper.orderByAsc("is_completed")
                .orderByDesc("delivery_date");

        Page<DeliveryInfo> deliveryInfoPage1 = deliveryInfoService.page(deliveryInfoPage, queryWrapper);
        // 判断查询结果是否为空
        if (deliveryInfoPage1.getRecords().isEmpty()) {
            return Result.success("暂无数据！");
        }

        List<DeliveryDetailVO> deliveryDetailVOList = deliveryInfoPage1.getRecords().stream().map(deliveryInfo -> {
            DeliveryDetailVO deliveryDetailVO = new DeliveryDetailVO();
            BeanUtil.copyProperties(deliveryInfo, deliveryDetailVO);
            deliveryDetailVO.setDeliveryId(deliveryInfo.getId());
            OrderInfo orderInfoDTO = orderInfoService.getById(deliveryInfo.getOrderId());
            String productIds = orderInfoDTO.getProductIds();
            String customerId = orderInfoDTO.getCustomerId();
            CustomerInfo customerDTO = customerInfoService.getById(customerId);
            String customerName = customerDTO.getName();
            String phoneNumber = customerDTO.getPhoneNumber();
            deliveryDetailVO.setCustomerName(customerName);
            deliveryDetailVO.setPhoneNumber(phoneNumber);
            deliveryDetailVO.setProductIds(productIds);

            return deliveryDetailVO;
        }).collect(Collectors.toList());
        // 构建分页 VO
        Page<DeliveryDetailVO> deliveryDetailVOPage = new Page<>();
        deliveryDetailVOPage.setRecords(deliveryDetailVOList);
        deliveryDetailVOPage.setCurrent(deliveryInfoPage1.getCurrent());
        deliveryDetailVOPage.setSize(deliveryInfoPage1.getSize());
        deliveryDetailVOPage.setTotal(deliveryInfoPage1.getTotal());

        // 返回分页结果
        return Result.success("查询成功", deliveryDetailVOPage);

    }

    @GetMapping("/{deliveryId}")
    public Result<DeliveryDetailVO> getByDeliveryId(@PathVariable("deliveryId")  String deliveryId){
        if (deliveryId == null || deliveryId.isEmpty()){
            return Result.error("交付ID不能为空！");
        }

        DeliveryInfo deliveryInfo = deliveryInfoService.getById(deliveryId);
        if (deliveryInfo == null ){
            return Result.error("交付信息不存在！");
        }

        DeliveryDetailVO deliveryDetailVO = new DeliveryDetailVO();

        BeanUtil.copyProperties(deliveryInfo,deliveryDetailVO);
        deliveryDetailVO.setDeliveryId(deliveryInfo.getId());
        OrderInfo orderInfoDTO = orderInfoService.getById(deliveryInfo.getOrderId());
        String productIds = orderInfoDTO.getProductIds();
        String customerId = orderInfoDTO.getCustomerId();
        CustomerInfo customerDTO = customerInfoService.getById(customerId);
        String customerName = customerDTO.getName();
        String phoneNumber = customerDTO.getPhoneNumber();
        deliveryDetailVO.setCustomerName(customerName);
        deliveryDetailVO.setPhoneNumber(phoneNumber);
        deliveryDetailVO.setProductIds(productIds);

        // 返回查询结果
        return Result.success("查询成功", deliveryDetailVO);
    }

    @PutMapping("/updateDelivery")
    public Result<Boolean> updateByDeliveryById(@RequestBody DeliveryDetailVO deliveryDetailVO){
        if (deliveryDetailVO == null || deliveryDetailVO.getDeliveryId() == null){
            return Result.error("交付ID不能为空！");
        }

        DeliveryInfo existingDelivery = deliveryInfoService.getById(deliveryDetailVO.getDeliveryId());
        if (existingDelivery == null){
            return Result.error("交付信息不存在！");
        }
        existingDelivery.setDeliveryPerson(deliveryDetailVO.getDeliveryPerson());
        existingDelivery.setDeliveryMethod(deliveryDetailVO.getDeliveryMethod());
        existingDelivery.setDeliveryDate(deliveryDetailVO.getDeliveryDate());
        existingDelivery.setDeliveryAddress(deliveryDetailVO.getDeliveryAddress());
        existingDelivery.setIsCompleted(deliveryDetailVO.getIsCompleted());
        existingDelivery.setDescription(deliveryDetailVO.getDescription());

        boolean deliveryUpdate = deliveryInfoService.updateById(existingDelivery);
        if (!deliveryUpdate) {
            return Result.error("交付更新失败，请重试！");
        }

        return Result.success("交付更新成功！");
    }
}
