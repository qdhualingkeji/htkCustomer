package com.hl.htk_customer.assistant;


import com.hl.htk_customer.model.ShopProduct;

/**
 * Created by 曹博 on 2016/6/7.
 * 购物车添加接口回调
 */
public interface ShopToDetailListener {
    /**
     * Type表示添加或减少
     * @param product
     * @param type
     */
    void onUpdateDetailList(ShopProduct product, String type);

    void onRemovePriduct(ShopProduct product);
}
