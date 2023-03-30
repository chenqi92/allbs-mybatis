package cn.allbs.mybatis.datascope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

/**
 * 类 DefaultPmsHandler
 *
 * @author ChenQi
 * @date 2023/3/30
 */
public class DefaultPmsHandler implements DataPmsHandler {
    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        return where;
    }
}
