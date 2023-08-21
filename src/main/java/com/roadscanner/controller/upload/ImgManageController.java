package com.roadscanner.controller.upload;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.service.upload.FileUploadService;

@Controller
public class ImgManageController implements PcwkLogger {

	@Autowired
	FileUploadService service;

	//@Autowired
	MessageVO messageVO;

	// default 생성자
	public ImgManageController() {
		LOG.debug("┌───────────────────────────┐");
		LOG.debug("│   ImgManageController()   │");
		LOG.debug("└───────────────────────────┘");
	}

	// feedback 화면
	@RequestMapping(value = "/graph")
	public String feedback() throws SQLException {
		return "graph";
	}

	// 분기별 피드백
	@RequestMapping(value = "/quarterlyFeedback", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String quarterlyFeedback(FileUploadVO inVO) throws SQLException {
		String jsonString = "";

		List<FileUploadVO> outVO = this.service.quarterlyFeedback(inVO);

		jsonString = new Gson().toJson(outVO);

		return jsonString;
	}

	// 누적 피드백
	@RequestMapping(value = "/totalFeedback", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String totalFeedback(FileUploadVO inVO) throws SQLException {
		String jsonString = "";

		FileUploadVO outVO = this.service.totalFeedback(inVO);

		jsonString = new Gson().toJson(outVO);

		return jsonString;
	}

	// 이미지 다 건 저장 (검토 여부 Update)
	@PostMapping(value = "/checkedUpdateMultiple")
	@ResponseBody
	public String checkedUpdateMultiple(String[] checkboxes) throws Exception {
		String jsonString = "";
		int flag = 0;
		FileUploadVO inVO = new FileUploadVO(); //checkedName에서 이름 꺼내와 넣을 빈 VO
		HashMap<String,String> successOrNot = new HashMap<String,String>(); //수정 여부 담을 해시맵
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│ checkedUpdateMultiple │");
		LOG.debug("│ checkedName           │" + checkboxes.toString());
		LOG.debug("└───────────────────────┘");

		try {
			for (String name:checkboxes) {
				inVO.setName(name);
				FileUploadVO resultVO = service.doSelectOne(inVO);
				flag = service.checkedUpdate(resultVO);
				if (1 == flag) { // 수정 성공
					successOrNot.put(name, "1");
				} else {
					successOrNot.put(name, "0");
					
				}
			}
			
			String message = "";
			if (successOrNot.containsValue("0")) { // 수정 실패한 이미지 있음
				flag = 0;
				message = "수정을 실패했습니다.";
			} else if (successOrNot.isEmpty()) {   // 체크된 이미지 없음
				flag = 0;
				message = "수정을 실패했습니다.";
			} else {                               // 체크된 이미지 전체 수정 성공
				flag = 1;
				message = "수정되었습니다.";
			}
			
			messageVO = new MessageVO(String.valueOf(flag), message);
			LOG.debug(messageVO.toString());
			jsonString = new Gson().toJson(flag);
			
		} catch (SQLException | IOException e) {
			String error = e.getMessage();
			LOG.debug("**Failed: "+error);
			throw e;
		}
		
		return jsonString;
	}

	// 이미지 다 건 삭제
	@PostMapping(value = "/doDeleteMultiple")
	@ResponseBody
	public String doDeleteMultiple(String[] checkboxes) throws SQLException, IOException {
		String jsonString = "";
		int flag = 0;
		FileUploadVO inVO = new FileUploadVO(); //checkedName에서 이름 꺼내와 넣을 빈 VO
		HashMap<String,String> successOrNot = new HashMap<String,String>(); //삭제 여부 담을 해시맵
		LOG.debug("┌─────────────────────┐");
		LOG.debug("│ doDeleteMultiple    │");
		LOG.debug("│ checkedName         │" + checkboxes.toString());
		LOG.debug("└─────────────────────┘");
		
		try {
			for (String name:checkboxes) {
				inVO.setName(name);
				FileUploadVO resultVO = service.doSelectOne(inVO);
				flag = service.doDelete(resultVO);
				if (1 == flag) { //삭제 성공
					successOrNot.put(name, "1");
				} else {
					successOrNot.put(name, "0");
					
				}
			}
			
			String message = "";
			if (successOrNot.containsValue("0")) { // 삭제 실패한 이미지 있음
				flag = 0;
				message = "삭제를 실패했습니다.";
			} else if (successOrNot.isEmpty()) {   // 체크된 이미지 없음
				flag = 0;
				message = "삭제를 실패했습니다.";
			} else {                               // 체크된 이미지 전체 삭제 성공
				flag = 1;
				message = "삭제되었습니다.";
			}
			
			messageVO = new MessageVO(String.valueOf(flag), message);
			LOG.debug(messageVO.toString());
			jsonString = new Gson().toJson(flag);
			
		} catch (SQLException | IOException e) {
			String error = e.getMessage();
			LOG.debug("**Failed: "+error);
			throw e;
		}
	
		return jsonString;
	}

	// 이미지 저장 (검토 여부 Update)
	@RequestMapping(value = "/checkedUpdate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String checkedUpdate(FileUploadVO inVO) throws Exception {
		String jsonString = "";
		LOG.debug("┌───────────────┐");
		LOG.debug("│ checkedUpdate │");
		LOG.debug("│ inVO          │" + inVO);
		LOG.debug("└───────────────┘");

		try {
			inVO = service.doSelectOne(inVO);
			int flag = service.checkedUpdate(inVO);

			String message = "";
			if (1 == flag) { // 삭제 성공
				message = "저장되었습니다.";
			} else { // 삭제 실패
				message = "저장을 실패했습니다.";
			}

			messageVO = new MessageVO(String.valueOf(flag), message);
			jsonString = new Gson().toJson(messageVO);

		} catch (SQLException | IOException e) {
			String error = e.getMessage();
			LOG.debug("Failed: " + error);
			throw e;
		}

		return jsonString;
	}

	// 이미지 삭제
	@RequestMapping(value = "/doDelete", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doDelete(FileUploadVO inVO) throws SQLException, IOException {
		String jsonString = "";

		LOG.debug("┌─────────────┐");
		LOG.debug("│ doDelete    │");
		LOG.debug("│ inVO        │" + inVO);
		LOG.debug("└─────────────┘");

		try {
			int flag = this.service.doDelete(inVO);

			String message = "";
			if (1 == flag) { // 삭제 성공
				message = "삭제되었습니다.";
			} else { // 삭제 실패
				message = "삭제를 실패했습니다.";
			}

			MessageVO messageVO = new MessageVO(String.valueOf(flag), message);
			jsonString = new Gson().toJson(messageVO);

		} catch (SQLException | IOException e) {
			String error = e.getMessage();
			LOG.debug("**Failed: " + error);
			throw e;
		}

		return jsonString;
	}

	// 이미지 단 건 상세 조회
	@RequestMapping(value = "/doSelectOne", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doSelectOne(FileUploadVO inVO) throws SQLException {
		String jsonString = "";

		LOG.debug("┌──────────────┐");
		LOG.debug("│ doSelectOne  │");
		LOG.debug("│ inVO         │" + inVO);
		LOG.debug("└──────────────┘");

		FileUploadVO outVO = this.service.doSelectOne(inVO);

		jsonString = new Gson().toJson(outVO);

		return jsonString;
	}

	// 이미지 목록 관리 화면
	@RequestMapping(value = "/imgManagement")
	public String imgManagement(@RequestParam(name = "category", required = false, defaultValue = "0") String category,
								FileUploadVO inVO, Model model) throws SQLException {
		LOG.debug("┌────────────────┐");
		LOG.debug("│ imgManagement  │");
		LOG.debug("│ inVO           │" + inVO);
		LOG.debug("└────────────────┘");

		// 페이지 번호 초기값: 1
		if (null != inVO && inVO.getPageNo() == 0) {
			inVO.setPageNo(1);
		}
		// 페이지 사이즈 초기값: 9
		if (null != inVO && inVO.getPageSize() == 0) {
			inVO.setPageSize(9);
		}

		// 카테고리 값 주기
		int intCategory = Integer.parseInt(category);
		inVO.setCategory(intCategory);

		// 목록 조회
		List<FileUploadVO> list = this.service.doRetrieve(inVO);
		model.addAttribute("list", list);
		model.addAttribute("inVO", inVO);

		// 총 글 수
		int totalCnt = 0;
		if (null != list && list.size() > 0) {
			totalCnt = list.get(0).getTotalCnt();
			LOG.debug("totalCnt : " + totalCnt);
		}
		model.addAttribute("totalCnt", totalCnt);

		return "imgManagement";
	}

}
