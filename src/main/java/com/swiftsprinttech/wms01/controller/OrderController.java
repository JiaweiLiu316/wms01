package com.swiftsprinttech.wms01.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swiftsprinttech.wms01.config.TimeUtil;
import com.swiftsprinttech.wms01.domain.entity.*;
import com.swiftsprinttech.wms01.domain.vo.*;
import com.swiftsprinttech.wms01.service.*;
import com.swiftsprinttech.wms01.utils.IdGenerator;
import com.swiftsprinttech.wms01.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单表
 * @author jiawe
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private IProductInfoService productInfoService;
    @Autowired
    private ICustomerInfoService customerInfoService;
    @Autowired
    private ICustomerFavoriteGoodsInfoService customerFavoriteGoodsInfoService;
    @Autowired
    private ICustomerAddressInfoService customerAddressInfoService;
    @Autowired
    private IDeliveryInfoService deliveryInfoService;
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private IOrderItemService orderItemService;
    @Autowired
    private IPaymentInfoService paymentInfoService;


    /**
     * @description: 订单列表
     * @param: [page, size]
     * @return: org.springframework.http.ResponseEntity<com.swiftsprinttech.wms01.utils.Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.swiftsprinttech.wms01.domain.entity.OrderInfo>>>
     * @date: 2024/11/17 下午 9:06
     */
    @GetMapping("/getAllOrder")
    public Result<Page<OrderInfoVO>> getAllOrder(
            @RequestParam(value = "page") Integer page,
            @RequestParam(value = "size") Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        // 构建分页查询条件
        Page<OrderInfo> orderPage = new Page<>(page, size);
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("status")
                .orderByDesc("created_time");

        // 分页查询订单实体
        Page<OrderInfo> orderInfoPage = orderInfoService.page(orderPage, queryWrapper);

        // 判断查询结果是否为空
        if (orderInfoPage.getRecords().isEmpty()) {
            return Result.success("暂无数据！");
        }

        // 转换为 VO 对象
        List<OrderInfoVO> orderInfoVOList = orderInfoPage.getRecords().stream().map(orderInfo -> {
            OrderInfoVO orderInfoVO = new OrderInfoVO();
            orderInfoVO.setOrderId(orderInfo.getId());
            orderInfoVO.setOrderStatus(orderInfo.getStatus());
            orderInfoVO.setOrderCreatedTime(orderInfo.getCreatedTime());

            // 查询并设置用户信息
            CustomerInfoVO customerInfoVO = customerInfoService.getCustomerInfoById(orderInfo.getCustomerId());
            if (customerInfoVO != null) {
                customerInfoVO.setShippingAddress(orderInfo.getShippingAddress());
                orderInfoVO.setCustomerInfoVO(customerInfoVO);
            } else {
                // 如果找不到用户信息，设置默认的 CustomerInfoVO 或者不设置
                // 这里我们选择跳过设置，或者可以设置一个默认值
                orderInfoVO.setCustomerInfoVO(new CustomerInfoVO());
            }
            // 查询并设置交付信息
            DeliveryInfoVO deliveryInfoVO = deliveryInfoService.getDeliveryInfoById(orderInfo.getDeliveryId());
            orderInfoVO.setDeliveryInfoVO(deliveryInfoVO);

            // 查询并设置付款信息
            PaymentInfoVO paymentInfoVO = paymentInfoService.getPaymentInfoById(orderInfo.getPaymentId());
            orderInfoVO.setPaymentInfoVO(paymentInfoVO);
            // 处理其他信息（交付、付款、订单项等）
            List<OrderItemVO> orderItemVOList = orderItemService.getOrderItemsByOrderId(orderInfo.getId()).stream().map(orderItem -> {
                OrderItemVO orderItemVO = new OrderItemVO();
                orderItemVO.setOrderItemId(orderItem.getOrderItemId());
                orderItemVO.setOrderId(orderItem.getOrderId());
                orderItemVO.setOrderItemQuantity(orderItem.getOrderItemQuantity());
                orderItemVO.setOrderItemTotalPrice(orderItem.getOrderItemTotalPrice());

                ProductBasicInfoVO productBasicInfoVO = productInfoService.getProductBasicInfoById(orderItem.getProductBasicInfoVO().getProductId());
                orderItemVO.setProductBasicInfoVO(productBasicInfoVO);

                return orderItemVO;
            }).collect(Collectors.toList());
            orderInfoVO.setOrderItemVOList(orderItemVOList);

            return orderInfoVO;
        }).collect(Collectors.toList());
        if (search != null && !search.isEmpty()) {
            // 使用 filter 进行模糊查询
            orderInfoVOList = orderInfoVOList.stream()
                    .filter(orderInfoVO -> {
                        // 判断是否任意一个字段包含搜索关键词
                        boolean customerMatch = orderInfoVO.getCustomerInfoVO() != null
                                && (orderInfoVO.getCustomerInfoVO().getCustomerName() != null
                                && orderInfoVO.getCustomerInfoVO().getCustomerName().contains(search)
                                || orderInfoVO.getCustomerInfoVO().getPhoneNumber() != null
                                && orderInfoVO.getCustomerInfoVO().getPhoneNumber().contains(search));

                        boolean deliveryAddressMatch = orderInfoVO.getDeliveryInfoVO() != null
                                && orderInfoVO.getDeliveryInfoVO().getDeliveryAddress() != null
                                && orderInfoVO.getDeliveryInfoVO().getDeliveryAddress().contains(search);

                        boolean productMatch = orderInfoVO.getOrderItemVOList() != null
                                && orderInfoVO.getOrderItemVOList().stream().anyMatch(orderItemVO ->
                                orderItemVO.getProductBasicInfoVO() != null
                                        && orderItemVO.getProductBasicInfoVO().getProductName() != null
                                        && orderItemVO.getProductBasicInfoVO().getProductName().contains(search)
                        );

                        boolean deliveryMethodMatch = orderInfoVO.getDeliveryInfoVO() != null
                                && orderInfoVO.getDeliveryInfoVO().getDeliveryMethod() != null
                                && orderInfoVO.getDeliveryInfoVO().getDeliveryMethod().contains(search);

                        // 只要任意条件匹配，即返回该订单
                        return customerMatch || deliveryAddressMatch || productMatch || deliveryMethodMatch;
                    })
                    .collect(Collectors.toList());  // 重新收集匹配到的结果
        }

        // 构建分页 VO
        Page<OrderInfoVO> orderInfoVOPage = new Page<>();
        orderInfoVOPage.setRecords(orderInfoVOList);
        orderInfoVOPage.setCurrent(orderInfoPage.getCurrent());
        orderInfoVOPage.setSize(orderInfoPage.getSize());
        orderInfoVOPage.setTotal(orderInfoPage.getTotal());

        // 返回分页结果
        return Result.success("查询成功", orderInfoVOPage);
    }


    @PostMapping("/add")
    @Transactional
    public Result<Object> addOrderInfo(@RequestBody OrderInfoVO orderInfoVO){
        if (orderInfoVO == null ){
            return Result.error("入库失败！");
        }
        // 生成唯一的订单 ID
        String orderId = IdGenerator.generateId();
        String customerId;
        String deliverId = IdGenerator.generateId();
        String paymentId = IdGenerator.generateId();

        List<String> productIdList = new ArrayList<>();
        LocalDateTime orderCreatedTime = LocalDateTime.now();

        //        用户信息入库
        CustomerInfo customerInfo = new CustomerInfo();
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone_number",orderInfoVO.getCustomerInfoVO().getPhoneNumber());
        // 检查是否存在相同手机号的用户
        CustomerInfo existingCustomer = customerInfoService.getOne(queryWrapper);
        if (existingCustomer != null) {
            // 如果用户已存在，则使用现有用户的 ID
            customerId = existingCustomer.getId();
            BigDecimal totalAmount = existingCustomer.getRepurchaseAmount().add(orderInfoVO.getPaymentInfoVO().getAmountDue());
            existingCustomer.setRepurchaseAmount(totalAmount);
//            存在则更新复购金额
            customerInfoService.updateById(existingCustomer);
        } else {
            customerId = IdGenerator.generateId();
            // 如果用户不存在，则新建用户信息
            customerInfo.setId(customerId);
            customerInfo.setName(orderInfoVO.getCustomerInfoVO().getCustomerName());
            customerInfo.setPhoneNumber(orderInfoVO.getCustomerInfoVO().getPhoneNumber());
            customerInfo.setRepurchaseAmount(orderInfoVO.getPaymentInfoVO().getAmountDue());
            customerInfo.setShippingAddress(orderInfoVO.getCustomerInfoVO().getShippingAddress());
            customerInfoService.save(customerInfo);
        }

        // 检查并保存地址信息
        CustomerAddressInfo customerAddressInfo = new CustomerAddressInfo();
        customerAddressInfo.setCustomerId(customerId);
        customerAddressInfo.setAddress(orderInfoVO.getCustomerInfoVO().getShippingAddress());

        QueryWrapper<CustomerAddressInfo> addressWrapper = new QueryWrapper<>();
        addressWrapper.eq("customer_id", customerId)
                .eq("address", orderInfoVO.getCustomerInfoVO().getShippingAddress());

        CustomerAddressInfo existingAddress = customerAddressInfoService.getOne(addressWrapper);
        if (existingAddress == null) {
            // 地址不存在，插入新地址
            customerAddressInfoService.save(customerAddressInfo);
        }

        // 检查并保存喜欢的商品信息
        for (OrderItemVO orderItemVO : orderInfoVO.getOrderItemVOList()) {
            String orderItemId = IdGenerator.generateId();
            productIdList.add(orderItemVO.getProductBasicInfoVO().getProductId());
            CustomerFavoriteGoodsInfo favoriteGoodsInfo = new CustomerFavoriteGoodsInfo();
            favoriteGoodsInfo.setCustomerId(customerId);
            favoriteGoodsInfo.setProductId(orderItemVO.getProductBasicInfoVO().getProductId());

            QueryWrapper<CustomerFavoriteGoodsInfo> favoriteWrapper = new QueryWrapper<>();
            favoriteWrapper.eq("customer_id", customerId)
                    .eq("product_id", orderItemVO.getProductBasicInfoVO().getProductId());

            CustomerFavoriteGoodsInfo existingFavoriteGoods = customerFavoriteGoodsInfoService.getOne(favoriteWrapper);
            if (existingFavoriteGoods == null) {
                // 如果商品不在收藏中，插入新记录
                favoriteGoodsInfo.setProductId(orderItemVO.getProductBasicInfoVO().getProductId());
                customerFavoriteGoodsInfoService.save(favoriteGoodsInfo);
            }

            // 保存商品项 (OrderItem)
            OrderItem orderItem = new OrderItem();
            orderItem.setId(orderItemId);
            orderItem.setOrderId(orderId);
            orderItem.setProductId(orderItemVO.getProductBasicInfoVO().getProductId());
            orderItem.setQuantity(orderItemVO.getOrderItemQuantity());
            orderItem.setUnitPrice(orderItemVO.getProductBasicInfoVO().getProductUnitPrice());
            BigDecimal totalPrice = orderItemVO.getProductBasicInfoVO().getProductUnitPrice().multiply(new BigDecimal(orderItemVO.getOrderItemQuantity()));
            orderItem.setTotalPrice(totalPrice);
            orderItemService.save(orderItem);
        }

        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setId(paymentId);
        paymentInfo.setOrderId(orderId);
        paymentInfo.setPaymentMethod(orderInfoVO.getPaymentInfoVO().getPaymentMethod());
        paymentInfo.setPaymentDate(orderInfoVO.getPaymentInfoVO().getPaymentDate());
        paymentInfo.setAmountDue(orderInfoVO.getPaymentInfoVO().getAmountDue());
        paymentInfo.setAmountPaid(orderInfoVO.getPaymentInfoVO().getAmountPaid());
        if (orderInfoVO.getPaymentInfoVO().getAmountDue().compareTo(orderInfoVO.getPaymentInfoVO().getAmountPaid()) == 0){
            orderInfoVO.getPaymentInfoVO().setIsCompleted(true);
        }else {
            orderInfoVO.getPaymentInfoVO().setIsCompleted(false);
        }
        paymentInfo.setIsCompleted(orderInfoVO.getPaymentInfoVO().getIsCompleted());
        paymentInfoService.save(paymentInfo);

        //        交付信息入库
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setOrderId(orderId);
        deliveryInfo.setId(deliverId);
        deliveryInfo.setDeliveryMethod(orderInfoVO.getDeliveryInfoVO().getDeliveryMethod());
        deliveryInfo.setDeliveryAddress(orderInfoVO.getDeliveryInfoVO().getDeliveryAddress());
        deliveryInfo.setDeliveryDate(orderInfoVO.getDeliveryInfoVO().getDeliveryDate());
        deliveryInfo.setDeliveryPerson(orderInfoVO.getDeliveryInfoVO().getDeliveryPerson());
        deliveryInfo.setDescription(orderInfoVO.getDeliveryInfoVO().getDescription());
        deliveryInfo.setIsCompleted(orderInfoVO.getDeliveryInfoVO().getIsCompleted());
        deliveryInfoService.save(deliveryInfo);

        // 保存订单信息
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setCustomerId(customerId);
        orderInfo.setDeliveryId(deliverId);
        orderInfo.setPaymentId(paymentId);
        orderInfo.setShippingAddress(orderInfoVO.getDeliveryInfoVO().getDeliveryAddress());
        orderInfo.setProductIds(String.valueOf(productIdList));
        if (orderInfoVO.getPaymentInfoVO().getIsCompleted() && orderInfoVO.getDeliveryInfoVO().getIsCompleted()){
            orderInfoVO.setOrderStatus(true);
        }else {
            orderInfoVO.setOrderStatus(false);
        }
        orderInfo.setStatus(orderInfoVO.getOrderStatus());
        orderInfo.setCreatedTime(TimeUtil.format(orderCreatedTime));
        orderInfoService.save(orderInfo);

        return Result.success("入库成功！");
    }

    /**
     * @description: 根据订单ID查询订单详情
     * @param: [orderId]
     * @return: org.springframework.http.ResponseEntity<com.swiftsprinttech.wms01.utils.Result<com.swiftsprinttech.wms01.domain.entity.OrderInfoVO>>
     * @date: 2024/11/18 下午 4:15
     */
    @GetMapping("/getOrderById/{orderId}")
    public Result<OrderInfoVO> getOrderById(@PathVariable("orderId") String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return Result.error("订单ID不能为空！");
        }

        // 查询订单实体
        OrderInfo orderInfo = orderInfoService.getById(orderId);
        if (orderInfo == null) {
            return Result.error("订单不存在！");
        }

        // 构建订单 VO
        OrderInfoVO orderInfoVO = new OrderInfoVO();
        orderInfoVO.setOrderId(orderInfo.getId());
        orderInfoVO.setOrderStatus(orderInfo.getStatus());
        orderInfoVO.setOrderCreatedTime(orderInfo.getCreatedTime());

        // 查询并设置用户信息
        CustomerInfoVO customerInfoVO = customerInfoService.getCustomerInfoById(orderInfo.getCustomerId());
        customerInfoVO.setShippingAddress(orderInfo.getShippingAddress());
        orderInfoVO.setCustomerInfoVO(customerInfoVO);

        // 查询并设置交付信息
        DeliveryInfoVO deliveryInfoVO = deliveryInfoService.getDeliveryInfoById(orderInfo.getDeliveryId());
        orderInfoVO.setDeliveryInfoVO(deliveryInfoVO);

        // 查询并设置付款信息
        PaymentInfoVO paymentInfoVO = paymentInfoService.getPaymentInfoById(orderInfo.getPaymentId());
        orderInfoVO.setPaymentInfoVO(paymentInfoVO);

        // 查询并设置订单项
        List<OrderItemVO> orderItemVOList = orderItemService.getOrderItemsByOrderId(orderInfo.getId()).stream().map(orderItem -> {
            OrderItemVO orderItemVO = new OrderItemVO();
            orderItemVO.setOrderItemId(orderItem.getOrderItemId());
            orderItemVO.setOrderId(orderItem.getOrderId());
            orderItemVO.setOrderItemQuantity(orderItem.getOrderItemQuantity());
            orderItemVO.setOrderItemTotalPrice(orderItem.getOrderItemTotalPrice());

            // 查询并设置商品基本信息
            ProductBasicInfoVO productBasicInfoVO = productInfoService.getProductBasicInfoById(orderItem.getProductBasicInfoVO().getProductId());
            orderItemVO.setProductBasicInfoVO(productBasicInfoVO);

            return orderItemVO;
        }).collect(Collectors.toList());
        orderInfoVO.setOrderItemVOList(orderItemVOList);

        // 返回查询结果
        return Result.success("查询成功", orderInfoVO);
    }


    /**
     * @description: 更新订单信息
     * @param: orderInfoVO 包含更新内容的订单信息
     * @return: ResponseEntity<Result<?>>
     */
    @PutMapping("/updateOrder")
    @Transactional
    public Result<Object> updateOrder(@RequestBody OrderInfoVO orderInfoVO) {
        // 校验输入
        if (orderInfoVO == null || orderInfoVO.getOrderId() == null) {
            return Result.error("订单ID不能为空！");
        }

        // 查询订单是否存在
        OrderInfo existingOrder = orderInfoService.getById(orderInfoVO.getOrderId());
        if (existingOrder == null) {
            return Result.error("订单不存在！");
        }
        existingOrder.setShippingAddress(orderInfoVO.getDeliveryInfoVO().getDeliveryAddress());
        if(orderInfoVO.getPaymentInfoVO().getIsCompleted() && orderInfoVO.getDeliveryInfoVO().getIsCompleted()){
            orderInfoVO.setOrderStatus(true);
        }else {
            orderInfoVO.setOrderStatus(false);
        }
        existingOrder.setStatus(orderInfoVO.getOrderStatus());
        // 获取用户ID
        String customerId = orderInfoVO.getCustomerInfoVO().getCustomerId();
        // 更新用户信息
        if (orderInfoVO.getCustomerInfoVO() != null) {
            // 检查用户是否存在
            CustomerInfo existingCustomer = customerInfoService.getById(customerId);
            if (existingCustomer == null) {
                return Result.error("用户不存在！");
            }
            existingCustomer.setName(orderInfoVO.getCustomerInfoVO().getCustomerName());
            existingCustomer.setPhoneNumber(orderInfoVO.getCustomerInfoVO().getPhoneNumber());
            existingCustomer.setShippingAddress(orderInfoVO.getCustomerInfoVO().getShippingAddress());
            customerInfoService.updateById(existingCustomer);
        }

        // 更新支付信息
        if (orderInfoVO.getPaymentInfoVO() != null) {
            String paymentId = orderInfoVO.getPaymentInfoVO().getPaymentId();
            // 检查支付信息是否存在
            PaymentInfo existingPayment = paymentInfoService.getById(paymentId);
            if (existingPayment == null) {
                return Result.error("支付信息不存在！");
            }
            // 更新支付信息
            existingPayment.setAmountPaid(orderInfoVO.getPaymentInfoVO().getAmountPaid());
            existingPayment.setAmountDue(orderInfoVO.getPaymentInfoVO().getAmountDue());
            existingPayment.setPaymentDate(orderInfoVO.getPaymentInfoVO().getPaymentDate());
            existingPayment.setPaymentMethod(orderInfoVO.getPaymentInfoVO().getPaymentMethod());
            existingPayment.setIsCompleted(orderInfoVO.getPaymentInfoVO().getIsCompleted());
            paymentInfoService.updateById(existingPayment);

        }

        // 更新配送信息
        if (orderInfoVO.getDeliveryInfoVO() != null) {
            String deliveryId = orderInfoVO.getDeliveryInfoVO().getDeliveryId();
            // 检查配送信息是否存在
            DeliveryInfo existingDelivery = deliveryInfoService.getById(deliveryId);
            if (existingDelivery == null) {
                return Result.error("配送信息不存在！");
            }

            // 获取配送地址
            String deliveryAddress = orderInfoVO.getDeliveryInfoVO().getDeliveryAddress();


            // 查找该用户是否已有该配送地址
            QueryWrapper<CustomerAddressInfo> addressQuery = new QueryWrapper<>();
            addressQuery.eq("customer_id", customerId).eq("address", deliveryAddress);
            CustomerAddressInfo existingAddress = customerAddressInfoService.getOne(addressQuery);

            // 如果用户地址表中没有该地址，插入该地址
            if (existingAddress == null) {
                CustomerAddressInfo newAddress = new CustomerAddressInfo();
                newAddress.setCustomerId(customerId);
                newAddress.setAddress(deliveryAddress);
                // 保存配送地址到用户地址表
                customerAddressInfoService.save(newAddress);
            }

            // 更新配送信息
            existingDelivery.setDeliveryAddress(deliveryAddress);
            existingDelivery.setDeliveryDate(orderInfoVO.getDeliveryInfoVO().getDeliveryDate());
            existingDelivery.setDeliveryMethod(orderInfoVO.getDeliveryInfoVO().getDeliveryMethod());
            existingDelivery.setDeliveryPerson(orderInfoVO.getDeliveryInfoVO().getDeliveryPerson());
            existingDelivery.setDescription(orderInfoVO.getDeliveryInfoVO().getDescription());
            existingDelivery.setIsCompleted(orderInfoVO.getDeliveryInfoVO().getIsCompleted());
            // 保存更新的配送信息
            deliveryInfoService.updateById(existingDelivery);
        }
        List<String> productIdList = new ArrayList<>();
        // 更新订单项（如果有修改）
        if (orderInfoVO.getOrderItemVOList() != null && !orderInfoVO.getOrderItemVOList().isEmpty()) {
            // 删除现有的订单项
            orderItemService.remove(new QueryWrapper<OrderItem>().eq("order_id", orderInfoVO.getOrderId()));
            // 插入新的订单项

            for (OrderItemVO orderItemVO : orderInfoVO.getOrderItemVOList()) {
                productIdList.add(orderItemVO.getProductBasicInfoVO().getProductId());
                OrderItem orderItem = new OrderItem();
                orderItem.setId(orderItemVO.getOrderItemId());
                orderItem.setOrderId(orderInfoVO.getOrderId());
                orderItem.setProductId(orderItemVO.getProductBasicInfoVO().getProductId());
                orderItem.setQuantity(orderItemVO.getOrderItemQuantity());
                orderItem.setUnitPrice(orderItemVO.getProductBasicInfoVO().getProductUnitPrice());
                orderItem.setTotalPrice(orderItemVO.getProductBasicInfoVO().getProductUnitPrice().multiply(BigDecimal.valueOf(orderItemVO.getOrderItemQuantity())));
                // 查找该用户是否已有该配送地址
                QueryWrapper<CustomerFavoriteGoodsInfo> goodsQuery = new QueryWrapper<>();
                goodsQuery.eq("customer_id", customerId).eq("product_id", orderItemVO.getProductBasicInfoVO().getProductId());
                CustomerFavoriteGoodsInfo goodsOne = customerFavoriteGoodsInfoService.getOne(goodsQuery);

                // 如果用户地址表中没有该地址，插入该地址
                if (goodsOne == null) {
                    CustomerFavoriteGoodsInfo newGoods = new CustomerFavoriteGoodsInfo();
                    newGoods.setCustomerId(customerId);
                    newGoods.setProductId(orderItemVO.getProductBasicInfoVO().getProductId());
                    // 保存配送地址到用户地址表
                    customerFavoriteGoodsInfoService.save(newGoods);
                }
                // 保存订单项
                orderItemService.save(orderItem);
            }
        }
        existingOrder.setProductIds(String.valueOf(productIdList));
        // 执行更新操作
        // 更新订单信息
        boolean success = orderInfoService.updateById(existingOrder);
        if (!success) {
            return Result.error("订单更新失败，请重试！");
        }

        return Result.success("订单更新成功！");
    }

}
