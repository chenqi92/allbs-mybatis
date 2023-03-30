package cn.allbs.mybatis.datascope;

import cn.allbs.mybatis.utils.PluginUtils;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.util.List;

/**
 * 数据权限处理器
 * 类 DataPermissionInterceptor
 *
 * @author ChenQi
 * @date 2023/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings({"rawtypes"})
public class DataPmsInterceptor extends JsqlParserSupport implements InnerInterceptor {
    private DataPmsHandler dataPermissionHandler;

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE || sct == SqlCommandType.SELECT) {
            if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
                return;
            }
            // 处理包含分页的情况
            if (ms.getId().contains("_mpCount") && InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId().replace("_mpCount", ""))) {
                return;
            }
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            mpBs.sql(parserMulti(mpBs.sql(), ms.getId()));
        }
    }

    /**
     * 查询
     */
    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            this.setWhere((PlainSelect) selectBody, (String) obj);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            List<SelectBody> selectBodyList = setOperationList.getSelects();
            selectBodyList.forEach(s -> this.setWhere((PlainSelect) s, (String) obj));
        }
    }

    /**
     * 新增
     */
    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * 删除
     */
    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        final Expression sqlSegment = getUpdateOrDeleteExpression(delete.getTable(), delete.getWhere(), (String) obj);
        if (null != sqlSegment) {
            delete.setWhere(sqlSegment);
        }
    }

    /**
     * 更新
     */
    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        final Expression sqlSegment = getUpdateOrDeleteExpression(update.getTable(), update.getWhere(), (String) obj);
        if (null != sqlSegment) {
            update.setWhere(sqlSegment);
        }
    }

    /**
     * 设置 where 条件
     *
     * @param plainSelect  查询对象
     * @param whereSegment 查询条件片段
     */
    protected void setWhere(PlainSelect plainSelect, String whereSegment) {
        Table table = (Table) plainSelect.getFromItem();
        Expression sqlSegment = dataPermissionHandler.getSqlSegment(table, plainSelect.getWhere(), whereSegment);
        if (null != sqlSegment) {
            plainSelect.setWhere(sqlSegment);
        }
    }

    protected Expression getUpdateOrDeleteExpression(final Table table, final Expression where, final String whereSegment) {
        return dataPermissionHandler.getSqlSegment(table, where, whereSegment);
    }

//    protected Expression andExpression(Table table, Expression where, final String whereSegment) {
//        //获得where条件表达式
//        final Expression expression = buildTableExpression(table, where, whereSegment);
//        if (expression == null) {
//            return where;
//        }
//        if (where != null) {
//            if (where instanceof OrExpression) {
//                return new AndExpression(new Parenthesis(where), expression);
//            } else {
//                return new AndExpression(where, expression);
//            }
//        }
//        return expression;
//    }
//
//    public Expression buildTableExpression(final Table table, final Expression where, final String whereSegment) {
//        // 只有新版数据权限处理器才会执行到这里
//        final MultiDataPermissionHandler handler = (MultiDataPermissionHandler) dataPermissionHandler;
//        return handler.getSqlSegment(table, where, whereSegment);
//    }
}
