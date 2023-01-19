package com.heima.model.wemedia.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 金宗文
 * @version 1.0
 */
@Data
@TableName("wm_news_material")
public class WmNewsMaterial implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 图文ID
     */
    @TableField("news_id")
    private Integer newsId;

    /**
     * 素材ID
     */
    @TableField("material_id")
    private Integer materialId;
    /**
     * 引用类型
     0 内容引用
     1 主图引用
     */
    @TableField("type")
    private Short type;
    /**
     * 引用排序
     */
    @TableField("ord")
    private Short ord;
}
