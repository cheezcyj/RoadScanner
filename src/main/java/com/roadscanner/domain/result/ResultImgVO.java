package com.roadscanner.domain.result;

import org.springframework.stereotype.Component;

import com.roadscanner.cmn.DTO;

@Component
public class ResultImgVO extends DTO {
	private int no;
	private String name;
	private String content;
	private String url;
	
	public ResultImgVO() {}

	public ResultImgVO(int no, String name, String content, String url) {
		super();
		this.no = no;
		this.name = name;
		this.content = content;
		this.url = url;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "ResultImgVO [no=" + no + ", name=" + name + ", content=" + content + ", url=" + url + "]";
	}
	
}