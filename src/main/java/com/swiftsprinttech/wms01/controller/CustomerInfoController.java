package com.swiftsprinttech.wms01.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swiftsprinttech.wms01.domain.entity.CustomerInfo;
import com.swiftsprinttech.wms01.domain.entity.OrderInfo;
import com.swiftsprinttech.wms01.domain.entity.ProductInfo;
import com.swiftsprinttech.wms01.domain.vo.CustomerAllInfoVO;
import com.swiftsprinttech.wms01.domain.vo.CustomerInfoVO;
import com.swiftsprinttech.wms01.domain.vo.ProductIdAndNameVO;
import com.swiftsprinttech.wms01.service.ICustomerAddressInfoService;
import com.swiftsprinttech.wms01.service.ICustomerFavoriteGoodsInfoService;
import com.swiftsprinttech.wms01.service.ICustomerInfoService;
import com.swiftsprinttech.wms01.utils.IdGenerator;
import com.swiftsprinttech.wms01.utils.Result;
import com.swiftsprinttech.wms01.utils.ResultUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author jiawe
 */
@RestController
@RequestMapping("/api/customerInfo")
public class CustomerInfoController {
    @Autowired
    private ICustomerInfoService customerInfoService;
    @Autowired
    private ICustomerAddressInfoService customerAddressInfoService;
    @Autowired
    private ICustomerFavoriteGoodsInfoService customerFavoriteGoodsInfoService;
    // 新增用戶
    @PostMapping("/add")
    public Result<Boolean> addCustomer(@RequestBody CustomerInfoVO customerInfoVO) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(IdGenerator.generateId());
        BeanUtils.copyProperties(customerInfoVO, customerInfo);
        CustomerInfo one = customerInfoService.getOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getPhoneNumber, customerInfoVO.getPhoneNumber()));
        if (one != null){
            return Result.error("用户已存在（电话号码相同），无需添加");
        }
        customerInfo.setName(customerInfoVO.getCustomerName());
        boolean isSaved = customerInfoService.save(customerInfo);
        if (isSaved) {
            return Result.success("用戶新增成功");
        } else {
            return Result.error("用戶新增失敗");
        }
    }

    @GetMapping("/pageList")
    public Result<Page<CustomerAllInfoVO>> getAllCustomer(
            @RequestParam Integer page,
            @RequestParam Integer size,
            @RequestParam(required = false) String search  // 搜索条件
    ) {
        try {
            // 创建分页对象
            Page<CustomerInfo> pageRequest = new Page<>(page, size);

            // 构建查询条件
            QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
            if (StringUtils.isNotBlank(search)) {
                queryWrapper.like("name", search)
                        .or().like("phone_number", search);
            }

            // 执行查询并获取分页结果
            Page<CustomerInfo> customerInfoPage = customerInfoService.page(pageRequest, queryWrapper);

            // 处理查询结果
            List<CustomerAllInfoVO> result = customerInfoPage
                    .getRecords().stream()
                    .map(customerInfo -> {
                        CustomerAllInfoVO customerAllInfoVO = new CustomerAllInfoVO();
                        customerAllInfoVO.setCustomerId(customerInfo.getId());
                        customerAllInfoVO.setCustomerName(customerInfo.getName());
                        customerAllInfoVO.setPhoneNumber(customerInfo.getPhoneNumber());
                        customerAllInfoVO.setRepurchaseAmount(customerInfo.getRepurchaseAmount());

                        // 获取顾客的地址信息并根据 search 条件进行过滤
                        List<String> addressByCustomerId = customerAddressInfoService.getAddressByCustomerId(customerInfo.getId());
                        List<String> filteredAddresses = Optional.ofNullable(addressByCustomerId)
                                .orElse(Collections.emptyList())  // 如果地址为 null，则使用空集合
                                .stream()
                                .filter(address -> address.toLowerCase().contains(search.toLowerCase()))  // 不区分大小写
                                .collect(Collectors.toList());
                        customerAllInfoVO.setAddress(filteredAddresses);

                        // 获取顾客收藏的商品信息并根据 search 条件进行过滤
                        List<ProductIdAndNameVO> goodsByCustomerId = customerFavoriteGoodsInfoService.getGoodsByCustomerId(customerInfo.getId());
                        List<ProductIdAndNameVO> filteredGoods = Optional.ofNullable(goodsByCustomerId)
                                .orElse(Collections.emptyList())  // 如果商品列表为 null，则使用空集合
                                .stream()
                                .filter(product -> product.getProductName().toLowerCase().contains(search.toLowerCase()))  // 不区分大小写
                                .collect(Collectors.toList());
                        customerAllInfoVO.setProductIdAndNameVOS(filteredGoods);

                        return customerAllInfoVO;
                    })
                    .collect(Collectors.toList());

            // 构建分页结果
            Page<CustomerAllInfoVO> resultPage = new Page<>();
            resultPage.setCurrent(customerInfoPage.getCurrent());
            resultPage.setSize(customerInfoPage.getSize());
            resultPage.setTotal(customerInfoPage.getTotal());
            resultPage.setRecords(result);

            // 返回成功的响应
            return Result.success("查询成功", resultPage);
        } catch (Exception e) {

            // 记录日志，便于排查问题
            return Result.error("出错");
        }
    }

    @GetMapping("/{customerId}")
    public Result<CustomerAllInfoVO> getCunstomerById(@PathVariable String customerId){
        if (customerId == null || customerId.isEmpty()){
            return Result.error("用户Id 不存在！");
        }
        CustomerInfo customerInfo = customerInfoService.getById(customerId);
       if(customerInfo == null){
           return Result.error("用户数据不存在！");
       }

        CustomerAllInfoVO customerAllInfoVO = new CustomerAllInfoVO();
        customerAllInfoVO.setCustomerId(customerInfo.getId());
        customerAllInfoVO.setCustomerName(customerInfo.getName());
        customerAllInfoVO.setPhoneNumber(customerInfo.getPhoneNumber());
        customerAllInfoVO.setRepurchaseAmount(customerInfo.getRepurchaseAmount());

        List<String> addressByCustomerId = customerAddressInfoService.getAddressByCustomerId(customerInfo.getId());
        customerAllInfoVO.setAddress(addressByCustomerId);
        List<ProductIdAndNameVO> goodsByCustomerId = customerFavoriteGoodsInfoService.getGoodsByCustomerId(customerInfo.getId());
        customerAllInfoVO.setProductIdAndNameVOS(goodsByCustomerId);
        return Result.success("查询成功",customerAllInfoVO);
    }
    @PutMapping("/editCustomer")
    public Result<Boolean> editCustomer(@RequestBody CustomerInfoVO customerInfoVO){
        if (customerInfoVO == null || customerInfoVO.getCustomerId().isEmpty()){
            return Result.error("用户Id不存在！");
        }
        CustomerInfo customerInfo = customerInfoService.getById(customerInfoVO.getCustomerId());
        if(customerInfo == null){
            return Result.error("用户数据不存在！");
        }
        customerInfo.setPhoneNumber(customerInfoVO.getPhoneNumber());
        customerInfoService.updateById(customerInfo);
        return Result.success("编辑成功");
    }
}
