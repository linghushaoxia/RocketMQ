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
package com.alibaba.rocketmq.namesrv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

import com.alibaba.rocketmq.common.MQVersion;
import com.alibaba.rocketmq.common.MixAll;
import com.alibaba.rocketmq.common.constant.LoggerName;
import com.alibaba.rocketmq.common.namesrv.NamesrvConfig;
import com.alibaba.rocketmq.remoting.netty.NettyServerConfig;
import com.alibaba.rocketmq.remoting.netty.NettySystemConfig;
import com.alibaba.rocketmq.remoting.protocol.RemotingCommand;
import com.alibaba.rocketmq.srvutil.ServerUtil;


/**
 * @author shijia.wxr
 */
public class NamesrvStartup {
    public static Properties properties = null;
    public static CommandLine commandLine = null;

    public static void main(String[] args) {
	 initEnv();
	 main0(args);
    }
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

    public static NamesrvController main0(String[] args) {
	//
        System.setProperty(RemotingCommand.RemotingVersionKey, Integer.toString(MQVersion.CurrentVersion));


        if (null == System.getProperty(NettySystemConfig.SystemPropertySocketSndbufSize)) {
            NettySystemConfig.socketSndbufSize = 4096;
        }


        if (null == System.getProperty(NettySystemConfig.SystemPropertySocketRcvbufSize)) {
            NettySystemConfig.socketRcvbufSize = 4096;
        }

        try {
            //PackageConflictDetect.detectFastjson();

            Options options = ServerUtil.buildCommandlineOptions(new Options());
            commandLine =ServerUtil.parseCmdLine("mqnamesrv", args, buildCommandlineOptions(options),
                            new PosixParser());
            if (null == commandLine) {
                System.exit(-1);
                return null;
            }


            final NamesrvConfig namesrvConfig = new NamesrvConfig();
            final NettyServerConfig nettyServerConfig = new NettyServerConfig();
            nettyServerConfig.setListenPort(9876);
            if (commandLine.hasOption('c')) {
                String file = commandLine.getOptionValue('c');
                if (file != null) {
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);
                    MixAll.properties2Object(properties, namesrvConfig);
                    MixAll.properties2Object(properties, nettyServerConfig);
                    System.out.println("load config properties file OK, " + file);
                    in.close();
                }
            }


            if (commandLine.hasOption('p')) {
                MixAll.printObjectProperties(null, namesrvConfig);
                MixAll.printObjectProperties(null, nettyServerConfig);
                System.exit(0);
            }

            MixAll.properties2Object(ServerUtil.commandLine2Properties(commandLine), namesrvConfig);

            if (null == namesrvConfig.getRocketmqHome()) {
                System.out.println("Please set the " + MixAll.ROCKETMQ_HOME_ENV
                        + " variable in your environment to match the location of the RocketMQ installation");
                System.exit(-2);
            }

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            configurator.doConfigure(namesrvConfig.getRocketmqHome() + "/conf/logback_namesrv.xml");
            final Logger log = LoggerFactory.getLogger(LoggerName.NamesrvLoggerName);


            MixAll.printObjectProperties(log, namesrvConfig);
            MixAll.printObjectProperties(log, nettyServerConfig);


            final NamesrvController controller = new NamesrvController(namesrvConfig, nettyServerConfig);
            boolean initResult = controller.initialize();
            if (!initResult) {
                controller.shutdown();
                System.exit(-3);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                private volatile boolean hasShutdown = false;
                private AtomicInteger shutdownTimes = new AtomicInteger(0);


                @Override
                public void run() {
                    synchronized (this) {
                        log.info("shutdown hook was invoked, " + this.shutdownTimes.incrementAndGet());
                        if (!this.hasShutdown) {
                            this.hasShutdown = true;
                            long begineTime = System.currentTimeMillis();
                            controller.shutdown();
                            long consumingTimeTotal = System.currentTimeMillis() - begineTime;
                            log.info("shutdown hook over, consuming time total(ms): " + consumingTimeTotal);
                        }
                    }
                }
            }, "ShutdownHook"));


            controller.start();

            String tip = "The Name Server boot success. serializeType=" + RemotingCommand.getSerializeTypeConfigInThisServer();
            log.info(tip);
            System.out.println(tip);
           //服务端配置信息
            System.out.println("the serverInfo:\n"+nettyServerConfig);

            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

    public static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("c", "configFile", true, "Name server config properties file");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("p", "printConfigItem", false, "Print all config item");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }
}
