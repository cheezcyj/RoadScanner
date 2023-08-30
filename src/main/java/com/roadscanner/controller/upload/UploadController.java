package com.roadscanner.controller.upload;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.result.ResultImgVO;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.service.result.ResultImgService;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.upload.RestTemplateService;

@Controller
@RequestMapping("/main")
public class UploadController implements PcwkLogger {
	
	@Autowired
	FileUploadService service;
	
	@Autowired
	ResultImgService imgService;
	
	@Autowired
	RestTemplateService restTemplateService;
	
	//@Autowired
	MessageVO messageVO;

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
	public String upload(@RequestParam(name = "imgName", required = false) String imageName, Model model,
					FileUploadVO inVO, ResultImgVO resultVO) throws SQLException {
		String flaskResult = "";
		int imgNo = 0;
		
		//직전 업로드한 파일url
		if (imageName != null) {
			model.addAttribute("thisName", imageName);
			String url = "https://roadscanner-test-bucket.s3.ap-northeast-2.amazonaws.com/"+imageName;
			model.addAttribute("thisUrl", url);
			
			// Flask API와 통신하여 결과값 받아오기
			flaskResult = restTemplateService.callFlaskApi(url);
			flaskResult = flaskResult.trim(); // 공백 및 줄바꿈 문자 제거
			
		} else {
			LOG.debug("session Connect Failed!");
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
		model.addAttribute("resultImg", resultImg);
		
		return "upload";
	}
	
    // 파일 업로드 처리
	@RequestMapping(value = "/fileUploaded", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
    public String uploadFile(@RequestParam("fileUpload") MultipartFile file, FileUploadVO inVO, HttpSession session) throws Exception {
		String result = "";
		String message = "";
		LOG.debug("┌─────────────────────────────────┐");
        LOG.debug("│uploadFile from Client to Service│");
        
		try {
			String saved = service.doSave(file, inVO);
			messageVO = new MessageVO();
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
				LOG.debug("│             Success             │");
				result = new Gson().toJson(saved);
			}
			
			LOG.debug("│success or not: "+messageVO.toString());
		} catch (SQLException | IOException e) {
			String error = e.getMessage();
			LOG.debug("│*****Failed: "+error);
			throw e;
		}
		
        LOG.debug("└─────────────────────────────────┘");
        
        return result;
    }
	
    // 피드백
	@RequestMapping(value = "/feedbackUpdate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
    public String feedbackUpdate(FileUploadVO inVO) throws SQLException {
		FileUploadVO newVO = service.doSelectOne(inVO);
		newVO.setCategory(inVO.getCategory());
		newVO.setU1(inVO.getU1());
		newVO.setU2(inVO.getU2());
		newVO.setU3(inVO.getU3());
		
		String result = "";
		LOG.debug("┌───────────────────────────────┐");
        LOG.debug("│feedback from Client to Service│");
        LOG.debug(newVO.toString());
        
        try {
			int flag = service.doUpdate(newVO);
			messageVO = new MessageVO();
			
			if (1 == flag) {
				messageVO.setMsgId("1");
				messageVO.setMsgContents("피드백 반영 성공");
				
				LOG.debug("│            Success            │");
			} else {
				messageVO.setMsgId("0");
				messageVO.setMsgContents("피드백 반영 실패");
			}
			
			LOG.debug(messageVO.toString());
			result = new Gson().toJson(messageVO);
		} catch (SQLException e) {
			String error = e.getMessage();
			LOG.debug("│*****Failed: "+error);
			throw e;
		}
        
        LOG.debug("└───────────────────────────────┘");
		return result;
	}
}
