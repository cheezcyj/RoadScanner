package com.roadscanner.domain.upload;

import org.springframework.stereotype.Component;

import com.roadscanner.cmn.DTO;

@Component
public class FileUploadVO extends DTO {
	private int idx; // 이미지 ID : upload_seq.NEXTVAL
	private String id; // 사용자 ID
	private int category; // 구분 : 기본(10), 좋아요(20), 싫어요(30), 문의사항(40)
	private String uploadDate; // 등록일
	private String name; // 저장파일명 : YYMMDDHH24MISS_원본파일명
	private String url; // URL : S3 저장 링크
	private int fileSize; // 파일크기
	private int checked; // 검토여부
	private int u1; // 싫어요 이유1
	private int u2; // 싫어요 이유2

	// Default 생성자
	public FileUploadVO() {
	}

	public FileUploadVO(int idx, String id, int category, String uploadDate, String name, String url, int fileSize,
			int checked, int u1, int u2) {
		super();
		this.idx = idx;
		this.id = id;
		this.category = category;
		this.uploadDate = uploadDate;
		this.name = name;
		this.url = url;
		this.fileSize = fileSize;
		this.checked = checked;
		this.u1 = u1;
		this.u2 = u2;
	}

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public String getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(String uploadDate) {
		this.uploadDate = uploadDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getChecked() {
		return checked;
	}

	public void setChecked(int checked) {
		this.checked = checked;
	}

	public int getU1() {
		return u1;
	}

	public void setU1(int u1) {
		this.u1 = u1;
	}

	public int getU2() {
		return u2;
	}

	public void setU2(int u2) {
		this.u2 = u2;
	}

	@Override
	public String toString() {
		return "FileUploadVO [idx=" + idx + ", id=" + id + ", category=" + category + ", uploadDate=" + uploadDate
				+ ", name=" + name + ", url=" + url + ", fileSize=" + fileSize + ", checked=" + checked + ", u1=" + u1
				+ ", u2=" + u2 + ", toString()=" + super.toString() + "]";
	}
	
}
