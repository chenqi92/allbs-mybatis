package cn.allbs.mybatis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类 MybatisProperties
 * </p>
 *
 * @author ChenQi
 * @since 2023/3/23 9:46
 */
@Data
@Component
@ConfigurationProperties("mybatis-plus")
public class MybatisProperties {

    /**
     * 是否打印可执行 sql
     */
    private boolean showSql = true;

    /**
     * 自定义汉字比英文字母的比例，用于sql打印时的展示效果，这边取个巧
     */
    private double chineRate = 2;

    /**
     * 数据权限是否开启
     */
    private boolean dataPms = false;

    /**
     * 自定义审计字段
     */

    private MetaMybatisProperties metaCustom;
}
