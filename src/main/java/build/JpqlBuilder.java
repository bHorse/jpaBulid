package build;

import java.util.*;

/**
 * jpql语句构建工具
 * Created by tby on 2016/9/1.
 *
 * 逻辑分析:组成jpql的方法
 */
public class JpqlBuilder {

    //jpql查询语句
    private StringBuilder jpql;
    //查询条件查询集合
    private List<Object> paramList;

    //and 语句片段,以及对应的参数集合
    private List<Map<String,List<Object>>> andPart;
    //or 语句片段,以及对应的参数集合
    private List<Map<String,List<Object>>>orPart;
    //一组or语句片段,以及对应的参数集合
    private List<Map<String,List<Object>>> groupOrPart;
    //having语句片段,以及对应的参数集合
    private Map<String,List<Object>> havingPart;
    //groupby语句片段
    private String  groupByPart;
    //用来排序字符串
    private String sortPart;


    public JpqlBuilder(String jpql){
        this.jpql=new StringBuilder(jpql);
    }

    /**
     * 构建JPQL语句
     * @return 最终的jpql语句
     * @throws ParamNumException 参数与?的数量不匹配
     */
    public String build() throws ParamNumException{
        paramList=new ArrayList<>();
        //先拼接and语句
        buildAnd();
        //再拼接or语句 ,最后所有的or要用()包起来
        buildOr();
        //拼接 groupOr语句,每一个都需要用()包起来
        buildGroupOr();
        //拼接 groupBy语句
        buildGroupBy();
        //拼接 having语句
        buildHaving(havingPart);
        //拼接 sort排序语句
        buildSort(sortPart);
        //将所有的?加上序号,异常表示?的数量与参数数量不相等
        if(!replaceChar())throw new ParamNumException("参数数量不匹配");
        return jpql.toString();
    }
    /**
     * 构建and条件
     * @param s and语句片段
     * @param param 参数
     * @return 构建对象
     */
    public JpqlBuilder addAnd(String s, Object...param) {
        if (param.length==0||param[0]==null)return this;
        if (andPart==null)andPart=new ArrayList<>();
        andPart.add(putMap(s,Arrays.asList(param)));
        return this;
    }

    /**
     * 构建or条件
     * @param s or语句片段
     * @param param 参数
     * @return 构建对象
     */
    public JpqlBuilder addOr(String s, Object...param) {
        if (param.length==0||param[0]==null)return this;
        if (orPart==null)orPart=new ArrayList<>();
        orPart.add(putMap(s,Arrays.asList(param)));
        return this;
    }

    /**
     * 构建需要用()包起来的条件
     * @param groupOr or语句组
     * @param param 参数
     * @return 构建对象
     */
    public JpqlBuilder addGroupOr(String groupOr, Object...param) {
        if (param.length==0||param[0]==null)return this;
        if (groupOrPart==null)groupOrPart=new ArrayList<>();
        groupOrPart.add(putMap(groupOr,Arrays.asList(param)));
        return this;
    }

    /**
     * 构建having条件
     * @param s having语句片段
     * @param param 参数
     * @return 构建对象
     */
    public JpqlBuilder addHaving(String s, Object...param) {
        if (param.length==0||param[0]==null)return this;
        havingPart=putMap(s,Arrays.asList(param));
        return this;
    }

    /**
     * 构建排序规则字段
     * @param s 排序语句片段
     * @return 构建对象
     */
    public JpqlBuilder addSort(String s) {
        sortPart=s;
        return this;
    }

    /**
     * 构建分组条件
     * @param s groupBy语句片
     * @return 构建对象
     */
    public JpqlBuilder addGroupBy(String s) {
        groupByPart=s;
        return this;
    }

    /**
     * 获取参数集合
     * @return 参数集合
     */
    public List<Object> getParamList() {
        return paramList;
    }

    /**
     * 将StringBuilder中的所有?替换成 ?N 形式
     */
    private boolean replaceChar() {
        int length = jpql.length(),index=0;
        for (int i=0;i<length;i++){
            if(jpql.charAt(i)!='?')continue;
            jpql.insert(++i,index++);
            //每插入一个字符,长度加1
            length++;
        }
        //如果参数与?的个数不相等,则该JPQL不能构建成功
        return paramList.size()==index;
    }

    private void buildGroupBy() {
        if (groupByPart==null)return;
        jpql.append(groupByPart);
    }

    private void buildSort(String sortPart) {
        if (sortPart==null)return;
        jpql.append(sortPart);
    }

    private void buildHaving(Map<String, List<Object>> param) {
        if (param==null)return;
        param.entrySet().forEach(x->{
            if (groupByPart==null)return;
            jpql.append(x.getKey());
            paramList.addAll(x.getValue());
        });
    }

    private void buildGroupOr() {
        if(groupOrPart==null) return;
        if (andPart==null&&orPart==null)jpql.append(" where ");
        else jpql.append("and ");
        for(Map<String,List<Object>> p:groupOrPart){
            p.forEach((k,v)->{
                jpql.append("(");
                jpql.append(k);
                jpql.append(")");
                jpql.append(" and");
                paramList.addAll(v);
            });
        }
        int length = jpql.length();
        jpql.delete(length -3,length);//将最后一个and删除
    }

    private void buildOr() {
        if(orPart==null) return;
        if (andPart==null)jpql.append(" where ( ");
        else jpql.append("and ( ");
        for(Map<String,List<Object>> p:orPart){
            p.forEach((k,v)->{
                jpql.append(k);
                jpql.append(" or");
                paramList.addAll(v);
            });
        }
        int length = jpql.length();
        jpql.delete(length -2,length);//将最后一个or删除
        jpql.append(")");
    }

    private void buildAnd() {
        if(andPart==null) return;
        jpql.append("where ");
        for(Map<String,List<Object>> p:andPart){
            p.forEach((k,v)->{
                        jpql.append(k);
                        jpql.append(" and");
                        paramList.addAll(v);
                    });
        }
        int length = jpql.length();
        jpql.delete(length -3,length);//将最后一个and删除
    }


    private Map<String,List<Object>> putMap(String s,List<Object> param){
        Map<String,List<Object>> map=new HashMap<>();
        map.put(s,param);
        return map;
    }
  /*  private boolean isAllNull(List<Map<String,List<Object>>> param){
        boolean flag = true;
        for(Map<String,List<Object>> p:param){
            p
        }
        return flag;
    }*/

    /**
     * jpql中的参数数量与实际的参数数量不匹配
     * 该异常已经不能恢复,故为非检异常
     * 该异常,上层代码没有捕获的必要,其他地方也不需要使用,故设置为私有
     * Created by tby on 2016/9/1.
     */
    private class ParamNumException extends RuntimeException {



        public ParamNumException(String message) {
            super(message);
        }
    }
}
