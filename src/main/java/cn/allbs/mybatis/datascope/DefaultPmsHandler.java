package cn.allbs.mybatis.datascope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Connection;

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

    @Override
    public void insertParameter(Insert insertStmt, BoundSql boundSql) {

    }

    @Override
    public void updateParameter(Update updateStmt, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {

    }
}
