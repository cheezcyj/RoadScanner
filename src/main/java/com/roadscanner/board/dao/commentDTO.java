package com.roadscanner.board.dao;

public class commentDTO {

	private int c_idx;
	private int q_id;
	private String r_id;
	private String c_contents;
	private String c_regDate;
	private String c_modDate;
	
	public commentDTO() {
		super();
	}

	public int getC_id() {
		return c_idx;
	}

	public void setC_id(int c_id) {
		this.c_idx = c_id;
	}

	public int getQ_id() {
		return q_id;
	}

	public void setQ_id(int q_id) {
		this.q_id = q_id;
	}

	public String getR_id() {
		return r_id;
	}

	public void setR_id(String r_id) {
		this.r_id = r_id;
	}

	public String getC_contents() {
		return c_contents;
	}

	public void setC_contents(String c_contents) {
		this.c_contents = c_contents;
	}

	public String getC_regDate() {
		return c_regDate;
	}

	public void setC_regDate(String c_regDate) {
		this.c_regDate = c_regDate;
	}

	public String getC_modDate() {
		return c_modDate;
	}

	public void setC_modDate(String c_modDate) {
		this.c_modDate = c_modDate;
	}

	@Override
	public String toString() {
		return "commentDTO [c_id=" + c_idx + ", q_id=" + q_id + ", r_id=" + r_id + ", c_contents=" + c_contents
				+ ", c_regDate=" + c_regDate + ", c_modDate=" + c_modDate + ", toString()=" + super.toString() + "]";
	}
	
}


