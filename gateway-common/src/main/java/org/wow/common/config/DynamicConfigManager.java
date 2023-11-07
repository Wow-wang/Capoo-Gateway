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

	//	规则集合 Node配置 -> 下面的所有Rule
	private ConcurrentHashMap<String /* ruleId */ , List<Rule>>  ruleMap = new ConcurrentHashMap<>();

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
		if(uniqueId == null) return;
		if(serviceInstanceSet.isEmpty()){
			serviceInstanceMap.remove(uniqueId);
			return;
		}
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
	public void putAllRule(Map<String,List<Rule>> rules) {
		ConcurrentHashMap<String,Rule> newPathMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,List<Rule>> newServiceMap = new ConcurrentHashMap<>();
		for(List<Rule> ruleList : rules.values()) {
			for (Rule rule : ruleList) {
				List<Rule> rulesTemp = newServiceMap.get(rule.getServiceId());
				if (rulesTemp == null) {
					rulesTemp = new ArrayList<>();
				}
				rulesTemp.add(rule);
				newServiceMap.put(rule.getServiceId(), rulesTemp);
				List<String> paths = rule.getPaths();
				for (String path : paths) {
					String key = rule.getServiceId() + "." + path;
					newPathMap.put(key, rule);
				}
			}
		}

		ruleMap = (ConcurrentHashMap<String, List<Rule>>) rules;
		// 1 对 1
		pathRuleMap = newPathMap;
		// 1 对 多
		serviceRuleMap = newServiceMap;

	}


	/** -------------------------------RuleMap--------------------------------------------- **/
	// 删除这个节点下所有的rule
	public void removeRuleNode(String node) {
		ruleMap.remove(node);
	}

	public List<Rule> getRuleNode(String node){
		return ruleMap.get(node);
	}

	public void updateRuleNode(String node,List<Rule> rules){
		ruleMap.get(node).addAll(rules);
	}


	public void putRuleNode(String node, List<Rule> rules){
		ruleMap.put(node,rules);
	}
	public void deleteRuleNode(String node){
		ruleMap.remove(node);
	}

	/** ---------------------------------pathRuleMap------------------------------------------- **/
	public Rule getRuleByPath(String path){
		return pathRuleMap.get(path);
	}

	public void putRuleByPath(String path,Rule rule){
		pathRuleMap.put(path,rule);
	}
	public void putAllRulesByPath(Map<String,Rule> rules){
		pathRuleMap.putAll(rules);
	}

	public void removeRuleByPath(String path){
		pathRuleMap.remove(path);
	}

	public void removeOneRuleByPath(String path){
		pathRuleMap.remove(path);
	}

	/** --------------------------------serviceRuleMap-------------------------------------------- **/
	public List<Rule> getRuleByServiceId(String serviceId){
		return serviceRuleMap.get(serviceId);
	}

	public void putRuleByServiceId(String serviceId, List<Rule> list){
		serviceRuleMap.put(serviceId,list);
	}
	
	public void putAllRulesByServiceId(Map<String,List<Rule>> rules){
		serviceRuleMap.putAll(rules);
	}

	public void removeOneRuleByServiceId(String serviceId,Rule rule){
		serviceRuleMap.get(serviceId).remove(rule);
	}
	public void removeAllRuleByServiceId(String serviceId){
		serviceRuleMap.remove(serviceId);
	}

	public void putNewRules(String node, List<Rule> ruleList){

		ConcurrentHashMap<String,Rule> newPathMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,List<Rule>> newServiceMap = new ConcurrentHashMap<>();
		for(Rule rule : ruleList){
			// Rule id
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
		putRuleNode(node,ruleList);
		putAllRulesByServiceId(newServiceMap);
		putAllRulesByPath(newPathMap);
	}

	public void deleteRules(String node){
		List<Rule> ruleList = getRuleNode(node);
		for(Rule rule : ruleList){
			String serviceId = rule.getServiceId();
			removeOneRuleByServiceId(serviceId,rule);
			List<String> paths = rule.getPaths();
			for (String path : paths) {
				String key = rule.getServiceId()+"."+path;
				removeOneRuleByPath(key);
			}
		}
		deleteRuleNode(node);
	}



}
