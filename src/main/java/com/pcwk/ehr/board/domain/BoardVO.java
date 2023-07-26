package com.pcwk.ehr.board.domain;

import com.pcwk.ehr.board.domain.DTO;

public class BoardVO extends DTO {   
	private int    seq      ;//순번 
	private String title    ;//제목
	private String contents ;//내용
	private String div      ;//구분
	private int    readCnt  ;//조회수
	private String regDt    ;//등록일
	private String regId    ;//등록자
	private String modDt    ;//수정일
	private String modId    ;//수정자

	public BoardVO() {}

	public BoardVO(int seq, String title, String contents, String div, int readCnt, String regDt, String regId,
			String modDt, String modId) {
		super();
		this.seq = seq;
		this.title = title;
		this.contents = contents;
		this.div = div;
		this.readCnt = readCnt;
		this.regDt = regDt;
		this.regId = regId;
		this.modDt = modDt;
		this.modId = modId;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public String getDiv() {
		return div;
	}

	public void setDiv(String div) {
		this.div = div;
	}

	public int getReadCnt() {
		return readCnt;
	}

	public void setReadCnt(int readCnt) {
		this.readCnt = readCnt;
	}

	public String getRegDt() {
		return regDt;
	}

	public void setRegDt(String regDt) {
		this.regDt = regDt;
	}

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getModDt() {
		return modDt;
	}

	public void setModDt(String modDt) {
		this.modDt = modDt;
	}

	public String getModId() {
		return modId;
	}

	public void setModId(String modId) {
		this.modId = modId;
	}

	@Override
	public String toString() {
		return "BoardVO [seq=" + seq + ", title=" + title + ", contents=" + contents + ", div=" + div + ", readCnt="
				+ readCnt + ", regDt=" + regDt + ", regId=" + regId + ", modDt=" + modDt + ", modId=" + modId
				+ ", toString()=" + super.toString() + "]";
	}
	
	
	
}  
