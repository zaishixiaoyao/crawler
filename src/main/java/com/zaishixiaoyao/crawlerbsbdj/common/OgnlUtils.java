package com.zaishixiaoyao.crawlerbsbdj.common;

import ognl.BooleanExpression;
import ognl.Ognl;
import ognl.OgnlException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OgnlUtils {

    //获取String类型
    public static String getString(String ognl , Map root){
        try {
            return Ognl.getValue(ognl , root).toString();
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
    }

    //获取Number类型
    public static Number getNumber(String ognl , Map root){ //Number是所有数字的父类
        Number result = null;
        try {
            Object val = Ognl.getValue(ognl , root);
            if(val != null){
                if(val instanceof  Number){ //如果是 1 ，返回1
                    result = (Number)val;
                }else if(val instanceof  String){ //如果是 “1” ，返回1
                    //将String类型以BigDecimal类型转化
                    result = new BigDecimal((String)val);
                }
            }
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
        return  result;
    }

    //获取Boolean类型
    public static Boolean getBoolean(String ognl , Map root){
        Boolean result = null;
        try {
            Object val = Ognl.getValue(ognl , root);
            if(val != null){
                if(val instanceof  Boolean){ //true或者false，直接返回
                    result = (Boolean) val;
                }else if(val instanceof  String){ //"true"，返回true ; "false"，返回false
                    //equalsIgnoreCase() 方法用于将字符串与指定的对象比较，不考虑大小写
                    result = ((String)val).equalsIgnoreCase("true")?true:false;
                }else if(val instanceof Number){ // 1 ，返回true ; 0 ， 返回false
                    //intValue()，数据类型转化，比如把Integer对象类型转化为Int基础数据类型
                    if(((Number)val).intValue() == 1){
                        result = true;
                    }else{
                        result = false;
                    }
                }
            }
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
        return  result;
    }

    //获取List集合，里面每一个元素都是Map
    public static List<Map<String,Object>> getListMap(String ognl , Map root){
        List<Map<String,Object>> list = null;
        try {
            list = (List)Ognl.getValue(ognl , root); //ognl是xx.yy，root是xx的上一个节点。比如Ognl.getValue("info.np" , ret)
            if(list == null){
                list = new ArrayList();
            }

        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    //获取list集合，里面每一个元素都是String
    public static List<String> getListString(String ognl , Map root){
        List<String> list = null;
        try {
            list = (List)Ognl.getValue(ognl , root);
            if(list == null){
                list = new ArrayList();
            }
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
