package org.wow.gateway.config.center.api;

import org.wow.common.config.Rule;

import java.util.List;

public interface RulesChangeListener {
    /**
     * 回调函数
     * @param rules
     */
    void onRulesChange(List<Rule> rules);
}
