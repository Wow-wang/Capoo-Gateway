package org.wow.gateway.config.center.api;

import org.wow.common.config.Rule;

import java.util.List;
import java.util.Map;

public interface RulesChangeListener {
    /**
     * 回调函数
     * @param rules
     */
    void onRulesChange(Map<String,List<Rule>> ruleMap);
}
