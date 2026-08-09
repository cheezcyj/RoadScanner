package com.roadscanner.service.upload;

public interface RestTemplateService {

	/**
	 * Requests side-effect-free image analysis. Implementations may retry this
	 * operation because analyzing the same immutable image URL is idempotent.
	 */
	String callFlaskApi(String imageUrl);

}
