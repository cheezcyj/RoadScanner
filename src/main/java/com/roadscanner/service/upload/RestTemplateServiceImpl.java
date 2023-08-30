package com.roadscanner.service.upload;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RestTemplateServiceImpl implements RestTemplateService {
	
	private String apiUrl = "http://172.31.44.211:5000/predict";
    
    private RestTemplate restTemplate = new RestTemplate();

	    @Autowired
	    public RestTemplateServiceImpl(RestTemplate restTemplate) {
	        this.restTemplate = restTemplate;
	    }

	    @Override
	    public String callFlaskApi(String imageUrl) {
	    	try {
	            // Flask API에 이미지 URL을 JSON 형태로 전달
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.APPLICATION_JSON);
	           
	            Map<String, String> requestMap = new HashMap<>();
	            requestMap.put("image_url", imageUrl);
	            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestMap, headers);
	            
	            // Flask API로 POST 요청 보내기
	            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

	            return response.getBody();
	        } catch (Exception e) {
	            return "Error communicating with Flask API";
	        }
	    }
	    
}
