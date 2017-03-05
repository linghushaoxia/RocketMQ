/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.linghushaoxia.rocketmq.tools.env;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.rocketmq.common.MixAll;

/**功能说明：
 * @author:linghushaoxia
 * @time:2017年1月14日下午3:07:41
 * @version:1.0
 * 为中国孱弱的技术，
 * 撑起一片自立自强的天空
 */
public class EnvUtil {
    /**
     * 
     * 功能说明: 初始化环境变量
     * @time:2017年1月13日下午10:15:13
     * @author:linghushaoxia
     * @exception:
     *
     */
    public static void initEnv() {
	/**
	 * 设置环境变量 只在当前运行时环境中生效
	 * 仅便于IDE调试使用
	 */
	Map<String, String> newEnv = new HashMap<String, String>();
	if (System.getenv().get("MixAll.ROCKETMQ_HOME_ENV") == null|| System.getenv("MixAll.ROCKETMQ_HOME_ENV") == "") {
	    /**
	     * 拼装ROCKETMQ_HOME
	     */
	    String userDir = System.getProperty("user.dir");
	    String ROCKETMQ_HOME = "";
	    // 简单的区分操作系统
	    if (userDir.contains("/")) {
		ROCKETMQ_HOME = userDir.substring(0, userDir.lastIndexOf("/"));
	    } else {
		ROCKETMQ_HOME = userDir.substring(0, userDir.lastIndexOf("\\"));
	    }
	    System.out.println("检测到未设置"+MixAll.ROCKETMQ_HOME_ENV+"环境变量,采用默认配置:"+ROCKETMQ_HOME);
	    newEnv.put(MixAll.ROCKETMQ_HOME_ENV, ROCKETMQ_HOME);
	}
	if (!newEnv.isEmpty()) {
	    setEnv(newEnv, false);
	}
    }
    /**
     * 
     * 功能说明:设置运行时环境的环境变量
     * 新的环境变量不会持久化到系统配置中,仅仅存在内存
     * @param newEnv
     * @param clearOldEnv
     * 是否清除旧的环境变量
     * @return Map<String,String>
     * 修改后的系统环境变量
     * @time:2017年1月12日下午5:23:16
     * @author:linghushaoxia
     * @see http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
     * @exception:
     *
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> setEnv(Map<String, String> newEnv,boolean clearOldEnv){
	try {
	    Class<?> unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
	    Map<String, String> env = System.getenv();
	    Field field = unmodifiableMap.getDeclaredField("m");
	    field.setAccessible(true);
	    Map<String, String> map = (Map<String, String>) field.get(env);
	    if (clearOldEnv) {
		map.clear();
	    }
	    map.putAll(newEnv);
	} catch (Exception e) {
	    e.printStackTrace();
	   }
	return System.getenv();
    }

}

/**
* 现实就是实现理想的过程。
* 
*/