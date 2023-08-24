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

	// 그래프 화면
	@RequestMapping(value = "/graph")
	public String feedback() throws SQLException {
		return "graph";
	}

	// 월별 피드백
	@RequestMapping(value = "/monthlyFeedback", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String monthlyFeedback(FileUploadVO inVO) throws SQLException {
		String jsonString = "";

		List<FileUploadVO> outVO = this.service.monthlyFeedback(inVO);

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
	public String imgManagement(@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
								@RequestParam(name = "category", required = false, defaultValue = "0") String category,
								FileUploadVO inVO, Model model) throws SQLException {
		LOG.debug("┌────────────────┐");
		LOG.debug("│ imgManagement  │");
		LOG.debug("│ inVO           │" + inVO);
		LOG.debug("└────────────────┘");

		// inVO 생성 및 초기값 설정
	    if (inVO.getPageNo() == 0) {
	        inVO.setPageNo(1); // 페이지 번호
	    }
	    if (inVO.getPageSize() == 0) {
	        inVO.setPageSize(9); // 페이지 사이즈
	    }
	    
		// 카테고리 값 주기
	    // 클라이언트에서 선택한 카테고리 값은 문자열로 전송됨
	    int intCategory = 0;
		if(category != null && !category.isEmpty()) {
			intCategory = Integer.parseInt(category);
		}
		inVO.setCategory(intCategory);
		
		// 목록 조회
	    List<FileUploadVO> list;
	    
	    if (intCategory != 0) { // 카테고리 값이 0이 아니면
	    	list = this.service.doRetrieveByCategory(inVO);
        } else {
        	list = this.service.doRetrieve(inVO);
        }

	    // 총 글 수
	    int totalCnt = 0;
	    if (list != null && !list.isEmpty()) {
	        totalCnt = list.get(0).getTotalCnt();
	    }
	    
	    // 총 페이지 수
	    int totalPages = (int) Math.ceil((double) totalCnt / inVO.getPageSize());
	    
	    // 페이지당 첫 번째 링크, 마지막 링크
	    int startPage = ((pageNo - 1) / 10) * 10 + 1;
        int endPage = startPage + 9;
	    
	    // 모델에 속성 추가
	    model.addAttribute("list", list);
	    model.addAttribute("inVO", inVO);
	    model.addAttribute("pageNo", inVO.getPageNo());
	    model.addAttribute("category", inVO.getCategory()); 
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("startPage", startPage);
	    model.addAttribute("endPage", endPage);
	    
		return "imgManagement";
	}

}
