package cn.allbs.mybatis.properties;

import lombok.Data;

/**
 * 类 MetaMybatisProperties
 * </p>
 *
 * @author ChenQi
 * @since 2023/3/23 10:26
 */
@Data
public class MetaMybatisProperties {

    /**
     * 创建者字段
     */
    private String createName = "createName";

    /**
     * 创建时间
     */
    private String createTime = "createTime";

    /**
     * 更新者字段
     */
    private String updateName = "updateName";

    /**
     * 更新时间
     */
    private String updateTime = "updateTime";

    /**
     * 逻辑删除字段
     */
    private String delFlg = "delFlg";
}
