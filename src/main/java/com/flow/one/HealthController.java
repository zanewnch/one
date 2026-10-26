package com.flow.one;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	/**
	 * 回報應用程式的健康狀態。
	 *
	 * @return 包含目前狀態的回應資料
	 */
	@GetMapping
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
