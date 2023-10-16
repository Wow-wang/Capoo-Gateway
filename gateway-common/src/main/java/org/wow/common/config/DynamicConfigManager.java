package org.wow.common.config;

import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 动态服务缓存配置管理类
 */
public class DynamicConfigManager {
	
	//	服务的定义集合：uniqueId代表服务的唯一标识
	private ConcurrentHashMap<String /* uniqueId */ , ServiceDefinition>  serviceDefinitionMap = new ConcurrentHashMap<>();
	
	//	服务的实例集合：uniqueId与一对服务实例对应
	private ConcurrentHashMap<String /* uniqueId */ , Set<ServiceInstance>>  serviceInstanceMap = new ConcurrentHashMap<>();

	//	规则集合
	private ConcurrentHashMap<String /* ruleId */ , Rule>  ruleMap = new ConcurrentHashMap<>();

	// 路径以及规则集合
	private ConcurrentHashMap<String /* 路径 */ , Rule> pathRuleMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String /* 服务名 */ , List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();

	private DynamicConfigManager() {
	}
	
	private static class SingletonHolder {
		private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
	}
	
	
	/***************** 	对服务定义缓存进行操作的系列方法 	***************/
	
	public static DynamicConfigManager getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	public void putServiceDefinition(String uniqueId, 
			ServiceDefinition serviceDefinition) {
		
		serviceDefinitionMap.put(uniqueId, serviceDefinition);;
	}
	
	public ServiceDefinition getServiceDefinition(String uniqueId) {
		return serviceDefinitionMap.get(uniqueId);
	}
	
	public void removeServiceDefinition(String uniqueId) {
		serviceDefinitionMap.remove(uniqueId);
	}
	
	public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
		return serviceDefinitionMap;
	}
	
	/***************** 	对服务实例缓存进行操作的系列方法 	***************/

	public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray){
		Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
		if(CollectionUtils.isEmpty(serviceInstances)){
			return Collections.emptySet();
		}
		if(gray){
			return serviceInstances.stream().filter(ServiceInstance::isGray).collect(Collectors.toSet());
		}
		return serviceInstances.stream().filter(((Predicate<ServiceInstance>) ServiceInstance::isGray).negate()).collect(Collectors.toSet());
	}
	
	public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		set.add(serviceInstance);
	}

	// 会覆盖原来的set集合 旧实例不可用直接覆盖
	public void addServiceInstance(String uniqueId, Set<ServiceInstance> serviceInstanceSet) {
		serviceInstanceMap.put(uniqueId, serviceInstanceSet);
	}
	
	public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		Iterator<ServiceInstance> it = set.iterator();
		while(it.hasNext()) {
			ServiceInstance is = it.next();
			if(is.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
				it.remove();
				break;
			}
		}
		set.add(serviceInstance);
	}
	
	public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		Iterator<ServiceInstance> it = set.iterator();
		while(it.hasNext()) {
			ServiceInstance is = it.next();
			if(is.getServiceInstanceId().equals(serviceInstanceId)) {
				it.remove();
				break;
			}
		}
	}
	
	public void removeServiceInstancesByUniqueId(String uniqueId) {
		serviceInstanceMap.remove(uniqueId);
	}
	
		
	/***************** 	对规则缓存进行操作的系列方法 	***************/
	
	public void putRule(String ruleId, Rule rule) {
		ruleMap.put(ruleId, rule);
	}

	/**
	 * 最开始BootStrap加载的时候 把规则刷新到管理器内部
	 * @param ruleList
	 */

	/**
	 * 单个Rule
	 * {
	 *       "id" : "user",
	 *       "name" : "user",
	 *       "protocol" : "http",
	 *       "serviceId" : "backend-http-server",
	 *       "prefix" : "/user",
	 *       "paths": [
	 *         "/http-server/ping","/user/update"
	 *       ],
	 *       "filterConfigs": [{
	 *           "id": "load_balance_filter",
	 *           "config": {
	 *             "load_balancer": "RoundRobin"
	 *           }
	 *         },{
	 *           "id" : "flow_ctl_filter"
	 *       }],
	 *       "retryConfig": {
	 *         "times":5
	 *       },
	 *       "flowCtlConfigs": [{
	 *         "type": "path",
	 *         "model" : "distributed",
	 *         "value" : "/http-server/ping",
	 *         "config": {
	 *           "duration": 20,
	 *           "permits": 2
	 *         }
	 *       }]
	 *       ,
	 *       "hystrixConfigs": [{
	 *         "path": "/http-server/ping",
	 *         "timeoutInMilliseconds": 5000,
	 *         "threadCoreSize": 2,
	 *         "fallbackResponse": "熔断超时"
	 *       }]
	 *     }
	 */
	public void putAllRule(List<Rule> ruleList) {
		ConcurrentHashMap<String,Rule> newRuleMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,Rule> newPathMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,List<Rule>> newServiceMap = new ConcurrentHashMap<>();
		for(Rule rule : ruleList){
			// Rule id
			newRuleMap.put(rule.getId(),rule);
			// Rule serviceId
//			System.out.println(rule.getServiceId());

			List<Rule> rules = newServiceMap.get(rule.getServiceId());
			if(rules == null){
				rules = new ArrayList<>();
			}
			rules.add(rule);
			newServiceMap.put(rule.getServiceId(),rules);

			// Rule path
			List<String> paths = rule.getPaths();
			for (String path : paths) {
				String key = rule.getServiceId()+"."+path;
				newPathMap.put(key,rule);
			}
		}
		ruleMap = newRuleMap;
		pathRuleMap = newPathMap;
		serviceRuleMap = newServiceMap;

//		Map<String, Rule> map = ruleList.stream()
//				.collect(Collectors.toMap(Rule::getId, r -> r));
//		ruleMap = new ConcurrentHashMap<>(map);
	}
	
	public Rule getRule(String ruleId) {
		return ruleMap.get(ruleId);
	}
	
	public void removeRule(String ruleId) {
		ruleMap.remove(ruleId);
	}
	
	public ConcurrentHashMap<String, Rule> getRuleMap() {
		return ruleMap;
	}

	public Rule getRuleByPath(String path){
		return pathRuleMap.get(path);
	}

	public List<Rule> getRuleByServiceId(String serviceId){
		return serviceRuleMap.get(serviceId);
	}

}
