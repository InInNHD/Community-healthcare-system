package com.community.healthcare.inventory.domain;

/** 库存流水方向。 */
public enum InventoryTransactionType {
    /** 验收入库。 */
    RECEIVE,
    /** 调剂发药出库。 */
    ISSUE
}
