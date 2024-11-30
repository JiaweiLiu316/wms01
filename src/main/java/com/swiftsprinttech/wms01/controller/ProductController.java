package com.swiftsprinttech.wms01.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swiftsprinttech.wms01.config.TimeUtil;
import com.swiftsprinttech.wms01.domain.entity.ProductInfo;
import com.swiftsprinttech.wms01.domain.vo.ProductIdAndNameVO;
import com.swiftsprinttech.wms01.service.IProductInfoService;
import com.swiftsprinttech.wms01.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品操作控制器
 * @author jiawei
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private IProductInfoService productInfoService;


    @GetMapping("/pageList")
    public Result<Page<ProductInfo>> getAllProducts(
            @RequestParam(value = "page", defaultValue = "1") Integer page,  // 页码，默认为1
            @RequestParam(value = "size", defaultValue = "10") Integer size,  // 每页数量，默认为10
            @RequestParam(value = "search", required = false) String search  // 模糊查询字段
    ) {
        try {
            // 创建分页对象
            Page<ProductInfo> pageRequest = new Page<>(page, size);

            // 创建 Lambda 查询构造器
            LambdaQueryWrapper<ProductInfo> queryWrapper = new LambdaQueryWrapper<>();

            // 如果 search 参数不为空，进行模糊查询
            if (search != null && !search.isEmpty()) {
                queryWrapper.like(ProductInfo::getId, search)
                        .or().like(ProductInfo::getName, search)
                        .or().like(ProductInfo::getStock, search)
                        .or().like(ProductInfo::getPrice, search)
                        .or().like(ProductInfo::getDescription, search)
                        .or().like(ProductInfo::getCategory, search)
                        .or().like(ProductInfo::getRegion, search);
            }

            // 执行查询并返回分页结果
            Page<ProductInfo> result = productInfoService.page(pageRequest, queryWrapper);

            // 返回成功的响应
            return Result.success("查询成功", result);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // 返回错误的响应
            return Result.error("出错");
        }
    }

    // 查询所有商品列表
    @GetMapping("/list")
    public Result<List<ProductInfo>> listAllProducts() {
        // 获取所有商品信息
        List<ProductInfo> productList = productInfoService.list();
        if (productList != null && !productList.isEmpty()) {
            return Result.success("查询成功", productList);
        } else {
            return Result.error("暂无商品信息");
        }
    }


    // 根据ID查询商品
    @GetMapping("/{id}")
    public Result<ProductInfo> getProductById(@PathVariable("id") String id) {
        ProductInfo product = productInfoService.getById(id);
        if (product != null) {
            return Result.success("查询成功", product);
        } else {
            return Result.error("商品未找到");
        }
    }

    // 新增商品
    @PostMapping("/add")
    public Result<Boolean> addProduct(@RequestBody ProductInfo productInfo) {
        LambdaQueryWrapper<ProductInfo> idLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ProductInfo> eq = idLambdaQueryWrapper.eq(ProductInfo::getId, productInfo.getId());
        ProductInfo idOne = productInfoService.getOne(eq);
        if (idOne != null ){
            return Result.error("标识重复,请确认后重新输入");
        }
        LambdaQueryWrapper<ProductInfo> nameLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ProductInfo> eq1 = nameLambdaQueryWrapper.eq(ProductInfo::getName, productInfo.getName());
        ProductInfo nameOne = productInfoService.getOne(eq1);
        if (nameOne != null){
            return Result.error("名称重复，请确认后重新输入");
        }
        productInfo.setCreatedTime(TimeUtil.format(LocalDateTime.now()));
        productInfo.setUpdatedTime(TimeUtil.format(LocalDateTime.now()));
        boolean isSaved = productInfoService.save(productInfo);
        if (isSaved) {
            return Result.success("商品新增成功");
        } else {
            return Result.error("商品新增失败");
        }
    }

    // 修改商品
    @PutMapping("/update")
    public Result<Boolean> updateProduct(@RequestBody ProductInfo productInfo) {
        productInfo.setUpdatedTime(TimeUtil.format(LocalDateTime.now()));
        boolean isUpdated = productInfoService.updateById(productInfo);
        if (isUpdated) {
            return Result.success("商品修改成功");
        } else {
            return Result.error("商品修改失败");
        }
    }

    // 删除商品
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteProduct(@PathVariable("id") String id) {
        boolean isDeleted = productInfoService.removeById(id);
        if (isDeleted) {
            return Result.success("商品删除成功");
        } else {
            return Result.error("商品删除失败");
        }
    }

    /**
     * @description: 获取产品标识和名称列表
     * @param: []
     * @return: org.springframework.http.ResponseEntity<com.swiftsprinttech.wms01.utils.Result<com.swiftsprinttech.wms01.domain.vo.ProductIdAndNameVO>>
     * @date: 2024/11/17 上午 1:27
     */
    @GetMapping("/productIdAndNameList")
    public Result<List<ProductIdAndNameVO>> getProductIdAndNameList(){
        // 获取所有商品信息
        List<ProductInfo> productList = productInfoService.list();
        // 转换为 ProductIdAndNameVO 列表
        List<ProductIdAndNameVO> productIdAndNameList = productList.stream()
                .map(product -> {
                    ProductIdAndNameVO vo = new ProductIdAndNameVO();
                    vo.setProductId(product.getId());
                    vo.setProductName(product.getName());
                    vo.setProductCategory(product.getCategory());
                    vo.setUnitPrice(product.getPrice());
                    vo.setProductStock(product.getStock());
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.success("查询成功",productIdAndNameList);

    }
}
