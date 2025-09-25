package com.zhaoxinms.contract.tools.stamp.config;

import java.io.InputStream;

public class StampRulesLoader {
	public static StampRulesConfig load() {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("stamp-rules.yml");
			if (in == null) {
				return new StampRulesConfig();
			}
			org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
			StampRulesConfig cfg = yaml.loadAs(in, StampRulesConfig.class);
			return cfg == null ? new StampRulesConfig() : cfg;
		} catch (Exception e) {
			return new StampRulesConfig();
		}
	}
}
