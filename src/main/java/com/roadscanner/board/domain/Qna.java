package com.roadscanner.board.domain;

import java.util.Date;

public class Qna {
    private int qIdx;
    private String rId;
    private int uIdx;
    private int qDiv;
    private int qReadCnt;
    private String qTitle;
    private String qContents;
    private Date qRegDate;
    private Date qModDate;
    private String aId;
    private String aContents;
    private Date aRegDate;
    private Date aModDate;
    
	public int getqIdx() {
		return qIdx;
	}
	public void setqIdx(int qIdx) {
		this.qIdx = qIdx;
	}
	public String getrId() {
		return rId;
	}
	public void setrId(String rId) {
		this.rId = rId;
	}
	public int getuIdx() {
		return uIdx;
	}
	public void setuIdx(int uIdx) {
		this.uIdx = uIdx;
	}
	public int getqDiv() {
		return qDiv;
	}
	public void setqDiv(int qDiv) {
		this.qDiv = qDiv;
	}
	public int getqReadCnt() {
		return qReadCnt;
	}
	public void setqReadCnt(int qReadCnt) {
		this.qReadCnt = qReadCnt;
	}
	public String getqTitle() {
		return qTitle;
	}
	public void setqTitle(String qTitle) {
		this.qTitle = qTitle;
	}
	public String getqContents() {
		return qContents;
	}
	public void setqContents(String qContents) {
		this.qContents = qContents;
	}
	public Date getqRegDate() {
		return qRegDate;
	}
	public void setqRegDate(Date qRegDate) {
		this.qRegDate = qRegDate;
	}
	public Date getqModDate() {
		return qModDate;
	}
	public void setqModDate(Date qModDate) {
		this.qModDate = qModDate;
	}
	public String getaId() {
		return aId;
	}
	public void setaId(String aId) {
		this.aId = aId;
	}
	public String getaContents() {
		return aContents;
	}
	public void setaContents(String aContents) {
		this.aContents = aContents;
	}
	public Date getaRegDate() {
		return aRegDate;
	}
	public void setaRegDate(Date aRegDate) {
		this.aRegDate = aRegDate;
	}
	public Date getaModDate() {
		return aModDate;
	}
	public void setaModDate(Date aModDate) {
		this.aModDate = aModDate;
	}
	
	@Override
	public String toString() {
		return "Qna [qIdx=" + qIdx + ", rId=" + rId + ", uIdx=" + uIdx + ", qDiv=" + qDiv + ", qReadCnt=" + qReadCnt
				+ ", qTitle=" + qTitle + ", qContents=" + qContents + ", qRegDate=" + qRegDate + ", qModDate="
				+ qModDate + ", aId=" + aId + ", aContents=" + aContents + ", aRegDate=" + aRegDate + ", aModDate="
				+ aModDate + ", toString()=" + super.toString() + "]";
	}

    
}

