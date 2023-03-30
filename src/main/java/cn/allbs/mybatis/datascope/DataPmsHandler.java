package cn.allbs.mybatis.datascope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

/**
 * 接口 AllbsDataPermissionHandler
 *
 * @author ChenQi
 * @date 2023/3/28
 */
public interface DataPmsHandler {

    /**
     * 获取数据权限 SQL 片段
     *
     * @param table             表相关信息
     * @param where             待执行 SQL Where 条件表达式
     * @param mappedStatementId Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @return JSqlParser 条件表达式，返回的条件表达式会覆盖原有的条件表达式
     */
    Expression getSqlSegment(final Table table, Expression where, String mappedStatementId);
}

