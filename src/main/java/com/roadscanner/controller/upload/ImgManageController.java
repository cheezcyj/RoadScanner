package com.roadscanner.controller.upload;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.cmn.AppLogger;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.service.upload.FileUploadService;

@Controller
public class ImgManageController implements AppLogger {
    private static final int IMAGE_PAGE_SIZE = 9;
    private static final int PAGE_BLOCK_SIZE = 10;
    private static final int MAX_BULK_ACTIONS = 100;

	private final FileUploadService service;

	@Autowired
	public ImgManageController(FileUploadService service) {
		this.service = service;
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
	@PostMapping(value = "/checkedUpdateMultiple", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public int checkedUpdateMultiple(
			@RequestParam(value = "checkboxes", required = false) String[] checkboxes) throws Exception {
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│ checkedUpdateMultiple │");
		LOG.debug("└───────────────────────┘");
		return processBulkAction(checkboxes, false);
	}

	// 이미지 다 건 삭제
	@PostMapping(value = "/doDeleteMultiple", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public int doDeleteMultiple(
			@RequestParam(value = "checkboxes", required = false) String[] checkboxes)
			throws SQLException, IOException {
		LOG.debug("┌─────────────────────┐");
		LOG.debug("│ doDeleteMultiple    │");
		LOG.debug("└─────────────────────┘");
		return processBulkAction(checkboxes, true);
	}

	// 이미지 저장 (검토 여부 Update)
	@RequestMapping(value = "/checkedUpdate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String checkedUpdate(FileUploadVO inVO) throws Exception {
		String jsonString = "";
		LOG.debug("┌───────────────┐");
		LOG.debug("│ checkedUpdate │");
		LOG.debug("└───────────────┘");

		try {
			inVO = service.doSelectOne(inVO);
			if (inVO == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
			}
			int flag = service.checkedUpdate(inVO);

			String message = "";
			if (1 == flag) { // 삭제 성공
				message = "저장되었습니다.";
			} else { // 삭제 실패
				message = "저장을 실패했습니다.";
			}

			MessageVO messageVO = new MessageVO(String.valueOf(flag), message);
			jsonString = new Gson().toJson(messageVO);

		} catch (SQLException | IOException e) {
			throw e;
		}

		return jsonString;
	}

	// 이미지 삭제
	@PostMapping(value = "/doDelete", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doDelete(FileUploadVO inVO) throws SQLException, IOException {
		String jsonString = "";

		LOG.debug("┌─────────────┐");
		LOG.debug("│ doDelete    │");
		LOG.debug("└─────────────┘");

		try {
			FileUploadVO storedFile = this.service.doSelectOne(inVO);
			if (storedFile == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
			}
			int flag = this.service.doDelete(storedFile);

			String message = "";
			if (1 == flag) { // 삭제 성공
				message = "삭제되었습니다.";
			} else { // 삭제 실패
				message = "삭제를 실패했습니다.";
			}

			MessageVO messageVO = new MessageVO(String.valueOf(flag), message);
			jsonString = new Gson().toJson(messageVO);

		} catch (SQLException | IOException e) {
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
		LOG.debug("└──────────────┘");

		FileUploadVO outVO = this.service.doSelectOne(inVO);
		if (outVO == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}

		jsonString = new Gson().toJson(outVO);

		return jsonString;
	}

	// 이미지 목록 관리 화면
	@RequestMapping(value = "/imgManagement")
	public String imgManagement(@RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
								@RequestParam(name = "category", defaultValue = "0") int category,
								FileUploadVO inVO, Model model) throws SQLException {
		LOG.debug("┌────────────────┐");
		LOG.debug("│ imgManagement  │");
		LOG.debug("└────────────────┘");

		int requestedPage = Math.max(1, pageNo);
		inVO.setPageNo(requestedPage);
		inVO.setPageSize(IMAGE_PAGE_SIZE);
		inVO.setCategory(isManagedCategory(category) ? category : 0);

		List<FileUploadVO> list = retrieveImages(inVO);
		if ((list == null || list.isEmpty()) && requestedPage > 1) {
			inVO.setPageNo(1);
			list = retrieveImages(inVO);
		}

	    // 총 글 수
	    int totalCnt = 0;
	    if (list != null && !list.isEmpty()) {
	        totalCnt = list.get(0).getTotalCnt();
	    }
	    
	    // 총 페이지 수
	    int totalPages = Math.max(1,
	            (int) Math.ceil((double) totalCnt / inVO.getPageSize()));
	    int normalizedPage = Math.min(requestedPage, totalPages);
	    if (normalizedPage != inVO.getPageNo()) {
	    	inVO.setPageNo(normalizedPage);
	    	list = retrieveImages(inVO);
	    }
	    

	    int startPage = ((normalizedPage - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
	    int endPage = startPage + PAGE_BLOCK_SIZE - 1;
	    
        if (endPage > totalPages) {
            endPage = totalPages;
        }
        
        int prevBlock = startPage - 1; // 이전 블럭의 마지막 페이지
        int nextBlock = endPage + 1; // 다음 블럭의 첫 페이지
        
        if (prevBlock < 1) {
            prevBlock = 1; // 이전 블록이 없는 경우 1로 설정
        }
        if (nextBlock > totalPages) {
            nextBlock = totalPages;
        }
        
	    // 모델에 속성 추가
	    model.addAttribute("list", list);
	    model.addAttribute("inVO", inVO);
	    model.addAttribute("pageNo", inVO.getPageNo());
	    model.addAttribute("category", inVO.getCategory()); 
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("startPage", startPage);
	    model.addAttribute("endPage", endPage);
	    model.addAttribute("prevBlock", prevBlock); 
	    model.addAttribute("nextBlock", nextBlock);    
	    
		return "imgManagement";
	}

	private List<FileUploadVO> retrieveImages(FileUploadVO criteria) throws SQLException {
		return criteria.getCategory() == 0
				? service.doRetrieve(criteria)
				: service.doRetrieveByCategory(criteria);
	}

	private boolean isManagedCategory(int category) {
		return category == 0 || category == 10 || category == 20 || category == 30;
	}

	private int processBulkAction(String[] names, boolean delete) throws SQLException, IOException {
		if (names == null || names.length == 0 || names.length > MAX_BULK_ACTIONS) {
			return 0;
		}

		Set<String> uniqueNames = new LinkedHashSet<String>();
		for (String name : names) {
			if (name == null || name.trim().isEmpty()) {
				return 0;
			}
			uniqueNames.add(name);
		}

		List<FileUploadVO> storedFiles = new ArrayList<FileUploadVO>(uniqueNames.size());
		for (String name : uniqueNames) {
			FileUploadVO lookup = new FileUploadVO();
			lookup.setName(name);
			FileUploadVO storedFile = service.doSelectOne(lookup);
			if (storedFile == null) {
				return 0;
			}
			storedFiles.add(storedFile);
		}

		for (FileUploadVO storedFile : storedFiles) {
			int changed = delete
					? service.doDelete(storedFile)
					: service.checkedUpdate(storedFile);
			if (changed != 1) {
				return 0;
			}
		}
		return 1;
	}

}
