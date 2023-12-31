package org.wow.core;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.utils.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 10:59
 **/
@Slf4j
public class ConfigLoader {

    private static final String CONFIG_FILE = "gateway.properties";
    private static final String ENV_PREFIX = "GATEWAY_";
    private static final String JVM_PREFIX = "gateway.";

    private static final ConfigLoader INSTANCE = new ConfigLoader();

    private ConfigLoader(){}

    public static ConfigLoader getInstance(){
        return INSTANCE;
    }

    private Config config;

    public static Config getConfig(){
        return INSTANCE.config;
    }

    /**
     * 运行参数  -> jvm参数 -> 环境变量 -> 配置文件 -> 配置对象默认值
     * @param args
     * @return
     */
    public Config load(String args[]){
        // 配置对象的默认值
        config = new Config();

        // 配置文件
        loadFromConfigFile();

        // 环境变量
        loadFromEnv();

        // jvm参数
        loadFromJvm();

        // 运行参数
        loadFromArgs(args);

        return config;
    }

    // java -jar YourApp.jar --port=8080 --host=localhost

    private void loadFromArgs(String[] args) {
        // --port=1234
        if(args != null && args.length > 0){
            Properties properties = new Properties();
            for(String arg : args){
                if(arg.startsWith("--") && arg.contains("=")){
                    properties.put(arg.substring(2,arg.indexOf("=")),
                            arg.substring(arg.indexOf("=")+1));
                }
            }
            PropertiesUtils.properties2Object(properties,config);
        }
    }


    private void loadFromJvm() {
        Properties properties = System.getProperties();
        PropertiesUtils.properties2Object(properties,config,JVM_PREFIX);
    }

    // export ENV_VARIABLE="my_value"
    private void loadFromEnv() {
        Map<String,String> env = System.getenv();
        Properties properties = new Properties();
        properties.putAll(env);
        PropertiesUtils.properties2Object(properties,config,ENV_PREFIX);
    }

    private void loadFromConfigFile(){
        // 相对路径解析
        InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if(inputStream != null){
            Properties properties = new Properties();
            try{
                properties.load(inputStream);

                /**
                 * 该方法的作用是将属性(Properties)转换为对象(Object)
                 * 如果 config 中已经存在相同属性名的字段，那么该字段的值将被 properties 中的值覆盖，
                 * 否则将保留 config 中的默认值。
                  */
                PropertiesUtils.properties2Object(properties,config);
            } catch (IOException e) {
                log.warn("load config file {} error",CONFIG_FILE,e);
            } finally {
                if(inputStream != null){
                    try{
                        inputStream.close();
                    }catch(IOException e){
                        //
                    }
                }
            }
        }
    }
}
