package com.roadscanner.board.code;

import com.roadscanner.board.domain.DTO;

public class CodeVO extends DTO {

	private String	codeId   ;//  코드ID
	private String	code     ;//  코드
	private String	codeNm   ;//  코드명
	private String	useYn    ;//  사용여부
	private String	regId    ;//  등록자아이디
	private String	regDt    ;//  등록일
	private String	modId    ;//  수정자아이디
	private String	modDt    ;//  수정일
	
	public CodeVO() {}

	public CodeVO(String codeId, String code, String codeNm, String useYn, String regId, String regDt, String modId,
			String modDt) {
		super();
		this.codeId = codeId;
		this.code = code;
		this.codeNm = codeNm;
		this.useYn = useYn;
		this.regId = regId;
		this.regDt = regDt;
		this.modId = modId;
		this.modDt = modDt;
	}

	public String getCodeId() {
		return codeId;
	}

	public void setCodeId(String codeId) {
		this.codeId = codeId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCodeNm() {
		return codeNm;
	}

	public void setCodeNm(String codeNm) {
		this.codeNm = codeNm;
	}

	public String getUseYn() {
		return useYn;
	}

	public void setUseYn(String useYn) {
		this.useYn = useYn;
	}

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getRegDt() {
		return regDt;
	}

	public void setRegDt(String regDt) {
		this.regDt = regDt;
	}

	public String getModId() {
		return modId;
	}

	public void setModId(String modId) {
		this.modId = modId;
	}

	public String getModDt() {
		return modDt;
	}

	public void setModDt(String modDt) {
		this.modDt = modDt;
	}

	@Override
	public String toString() {
		return "CodeVO [codeId=" + codeId + ", code=" + code + ", codeNm=" + codeNm + ", useYn=" + useYn + ", regId="
				+ regId + ", regDt=" + regDt + ", modId=" + modId + ", modDt=" + modDt + ", toString()="
				+ super.toString() + "]";
	}
	
	
}
