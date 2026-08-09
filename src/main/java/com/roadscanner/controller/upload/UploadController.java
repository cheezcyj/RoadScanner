package com.roadscanner.controller.upload;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.cmn.AppLogger;
import com.roadscanner.domain.result.ResultImgVO;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.result.ResultImgService;
import com.roadscanner.service.upload.AnalysisApiException;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.upload.RestTemplateService;

@Controller
@RequestMapping("/main")
public class UploadController implements AppLogger {

	private static final int ADMIN_GRADE = 2;
	private static final int INITIAL_UPLOAD_CATEGORY = 10;
	private static final int POSITIVE_FEEDBACK_CATEGORY = 20;
	private static final int NEGATIVE_FEEDBACK_CATEGORY = 30;
	
	@Autowired
	FileUploadService service;
	
	@Autowired
	ResultImgService imgService;
	
	@Autowired
	RestTemplateService restTemplateService;
	
	// default 생성자
	public UploadController() {
		LOG.debug("┌────────────────────────────┐");
        LOG.debug("│     UploadController()     │");
        LOG.debug("└────────────────────────────┘");
	}
	
	@RequestMapping(value = "/preUpload")
	public String preUpload() {
		return "preUpload";
	}
	
	// upload 화면
	@RequestMapping("/upload")
	//@RequestMapping(value = "/upload", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public String upload(@RequestParam(name = "idx") int uploadIdx, Model model,
					ResultImgVO resultVO, @SessionAttribute("user") MemberVO memberVO) throws SQLException {
		FileUploadVO lookup = new FileUploadVO();
		lookup.setIdx(uploadIdx);
		FileUploadVO storedUpload = service.doSelectOne(lookup);
		if (storedUpload == null || !isAnalysisResultCategory(storedUpload.getCategory())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found");
		}
		if (memberVO.getGrade() != ADMIN_GRADE && !memberVO.getId().equals(storedUpload.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upload access denied");
		}
		if (storedUpload.getName() == null || storedUpload.getUrl() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found");
		}

		String flaskResult = "";
		int imgNo = 0;
		
		// DB에 저장된 canonical URL만 분석 서버와 화면에 전달한다.
		model.addAttribute("upload", storedUpload);
		model.addAttribute("thisUrl", storedUpload.getUrl());
		flaskResult = restTemplateService.callFlaskApi(storedUpload.getUrl());
		if (flaskResult != null) {
			flaskResult = flaskResult.trim();
		}
		
		//피드백 원인을 리스트로
		List<String> reasonList = new ArrayList<String>(Arrays.asList("모양 인식 오류", "색깔 인식 오류", "그림/숫자 인식 오류"));
		model.addAttribute("reasons", reasonList);
		
		//결과 이미지 (결과를 정수
		try {
		    imgNo = Integer.parseInt(flaskResult);
		    resultVO.setNo(imgNo);
		} catch (NumberFormatException e) {
		    resultVO.setNo(404);
		}
		ResultImgVO resultImg = imgService.getResultImg(resultVO);
		if (resultImg == null) {
			throw AnalysisApiException.responseError(200, null);
		}
		model.addAttribute("resultImg", resultImg);
		
		return "upload";
	}
	
    // 파일 업로드 처리
	@RequestMapping(value = "/fileUploaded", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
    public String uploadFile(@RequestParam("fileUpload") MultipartFile file, FileUploadVO inVO,
                             @SessionAttribute("user") MemberVO memberVO) throws Exception {
		String result = "";
		String message = "";
		LOG.debug("┌─────────────────────────────────┐");
        LOG.debug("│uploadFile from Client to Service│");
        
		try {
			inVO.setId(memberVO.getId());
			inVO.setCategory(INITIAL_UPLOAD_CATEGORY);
			String saved = service.doSave(file, inVO);
			MessageVO messageVO = new MessageVO();
			if ("0".equals(saved)) {
				messageVO.setMsgId("0");
				messageVO.setMsgContents("업로드 실패");
				result = new Gson().toJson("업로드 실패");
			} else {
				messageVO.setMsgId("1");
				String fileName = file.getOriginalFilename();
				String shortFile = fileName.substring(0, fileName.indexOf("."));
				if (shortFile.length() > 6) {
					message = shortFile.substring(0, 6)+"...가 업로드되었습니다.";
					messageVO.setMsgContents(message);
				} else {
					message = shortFile+"가 업로드되었습니다.";
					messageVO.setMsgContents(message);
				}
				FileUploadVO lookup = new FileUploadVO();
				lookup.setName(saved);
				FileUploadVO storedUpload = service.doSelectOne(lookup);
				if (storedUpload == null || storedUpload.getIdx() <= 0
						|| !memberVO.getId().equals(storedUpload.getId())
						|| storedUpload.getCategory() != INITIAL_UPLOAD_CATEGORY) {
					throw new IllegalStateException("Saved upload record could not be resolved");
				}
				LOG.debug("│             Success             │");
				result = new Gson().toJson(storedUpload.getIdx());
			}
			
		} catch (SQLException | IOException e) {
			throw e;
		}
		
        LOG.debug("└─────────────────────────────────┘");
        
        return result;
    }
	
    // 피드백
	@RequestMapping(value = "/feedbackUpdate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
    public String feedbackUpdate(FileUploadVO inVO,
                                 @SessionAttribute("user") MemberVO memberVO) throws SQLException {
		FileUploadVO newVO = service.doSelectOne(inVO);
		if (newVO == null || newVO.getCategory() != INITIAL_UPLOAD_CATEGORY) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}
		if (memberVO.getGrade() != ADMIN_GRADE && !memberVO.getId().equals(newVO.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File access denied");
		}
		if (inVO.getCategory() != POSITIVE_FEEDBACK_CATEGORY
				&& inVO.getCategory() != NEGATIVE_FEEDBACK_CATEGORY) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feedback category");
		}
		validateFeedbackReasons(inVO);
		newVO.setCategory(inVO.getCategory());
		newVO.setU1(inVO.getU1());
		newVO.setU2(inVO.getU2());
		newVO.setU3(inVO.getU3());
		
		String result = "";
		LOG.debug("┌───────────────────────────────┐");
        LOG.debug("│feedback from Client to Service│");
        
		try {
			int flag = service.doUpdate(newVO);
			MessageVO messageVO = new MessageVO();
			
			if (1 == flag) {
				messageVO.setMsgId("1");
				messageVO.setMsgContents("피드백 반영 성공");
				
				LOG.debug("│            Success            │");
			} else {
				messageVO.setMsgId("0");
				messageVO.setMsgContents("피드백 반영 실패");
			}
			
			result = new Gson().toJson(messageVO);
		} catch (SQLException e) {
			throw e;
		}
        
        LOG.debug("└───────────────────────────────┘");
		return result;
	}

	private boolean isAnalysisResultCategory(int category) {
		return category == INITIAL_UPLOAD_CATEGORY
				|| category == POSITIVE_FEEDBACK_CATEGORY
				|| category == NEGATIVE_FEEDBACK_CATEGORY;
	}

	private void validateFeedbackReasons(FileUploadVO feedback) {
		int u1 = feedback.getU1();
		int u2 = feedback.getU2();
		int u3 = feedback.getU3();
		if (!isBinary(u1) || !isBinary(u2) || !isBinary(u3)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feedback reasons");
		}
		if (feedback.getCategory() == POSITIVE_FEEDBACK_CATEGORY && (u1 + u2 + u3 != 0)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Positive feedback cannot include reasons");
		}
		if (feedback.getCategory() == NEGATIVE_FEEDBACK_CATEGORY && (u1 + u2 + u3 == 0)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Negative feedback requires a reason");
		}
	}

	private boolean isBinary(int value) {
		return value == 0 || value == 1;
	}
}
