package com.roadscanner.controller.qna;

import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.domain.qna.QuestionCategory;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.PaginationDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.service.qna.AnswerService;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.user.UserService;

import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RequestMapping("/qna")
@Controller
public class QuestionController {

    private static final int ADMIN_GRADE = 2;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String CATEGORY_NOTICE = "notice";
    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_ANSWERED = "answered";
    private static final String CATEGORY_WAITING = "waiting";
    private static final String SEARCH_BOTH = "both";
    private static final String SEARCH_TITLE = "title";
    private static final String SEARCH_CONTENT = "content";

    private final QuestionService questionService;
    private final AnswerService answerService;
    
    private final FileUploadService fileUploadService;
    private final UserService userService;


    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "searchType", required = false) String searchType,
                        @RequestParam(value = "keyword", required = false) String keyword) {
        String normalizedCategory = normalizeBoardCategory(category);
        Integer categoryCode = boardCategoryCode(normalizedCategory);
        String normalizedSearchType = normalizeSearchType(searchType);
        String normalizedKeyword = normalizeKeyword(keyword);
        PaginationDTO pagination = new PaginationDTO(page, size);
        size = pagination.getSize();
        int totalQuestions = questionService.countBoard(
                categoryCode, normalizedSearchType, normalizedKeyword);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalQuestions / size));
        page = Math.min(pagination.getPage(), totalPages);
        pagination = new PaginationDTO(page, size);

        model.addAttribute("questions", questionService.findBoardWithPaging(
                pagination, categoryCode, normalizedSearchType, normalizedKeyword));
        addListModel(model, page, size, totalPages,
                normalizedCategory, normalizedSearchType, normalizedKeyword);
        addViewMode(model, "board", "/qna");

        return "qna/index";
    }

    @GetMapping("/my")
    public String myQuestions(Model model,
                              @SessionAttribute("user") MemberVO memberVO,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(value = "category", required = false) String category,
                              @RequestParam(value = "searchType", required = false) String searchType,
                              @RequestParam(value = "keyword", required = false) String keyword) {
        String normalizedCategory = normalizeInquiryCategory(category);
        Integer categoryCode = inquiryCategoryCode(normalizedCategory);
        String normalizedSearchType = normalizeSearchType(searchType);
        String normalizedKeyword = normalizeKeyword(keyword);
        PaginationDTO pagination = new PaginationDTO(page, size);
        size = pagination.getSize();
        int totalQuestions = questionService.countInquiriesByAuthor(
                memberVO.getId(), categoryCode, normalizedSearchType, normalizedKeyword);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalQuestions / size));
        page = Math.min(pagination.getPage(), totalPages);
        pagination = new PaginationDTO(page, size);

        model.addAttribute("questions",
                questionService.findInquiriesByAuthorWithPaging(memberVO.getId(), pagination,
                        categoryCode, normalizedSearchType, normalizedKeyword));
        addListModel(model, page, size, totalPages,
                normalizedCategory, normalizedSearchType, normalizedKeyword);
        addViewMode(model, "myInquiry", "/qna/my");
        return "qna/index";
    }

    @GetMapping("/inquiries")
    public String inquiries(Model model,
                            @SessionAttribute("user") MemberVO memberVO,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(value = "category", required = false) String category,
                            @RequestParam(value = "searchType", required = false) String searchType,
                            @RequestParam(value = "keyword", required = false) String keyword) {
        if (!isAdmin(memberVO)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrator access required");
        }

        String normalizedCategory = normalizeInquiryCategory(category);
        Integer categoryCode = inquiryCategoryCode(normalizedCategory);
        String normalizedSearchType = normalizeSearchType(searchType);
        String normalizedKeyword = normalizeKeyword(keyword);
        PaginationDTO pagination = new PaginationDTO(page, size);
        size = pagination.getSize();
        int totalQuestions = questionService.countInquiries(
                categoryCode, normalizedSearchType, normalizedKeyword);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalQuestions / size));
        page = Math.min(pagination.getPage(), totalPages);
        pagination = new PaginationDTO(page, size);

        model.addAttribute("questions", questionService.findInquiriesWithPaging(
                pagination, categoryCode, normalizedSearchType, normalizedKeyword));
        addListModel(model, page, size, totalPages,
                normalizedCategory, normalizedSearchType, normalizedKeyword);
        addViewMode(model, "adminInquiry", "/qna/inquiries");
        return "qna/index";
    }

    /**
     * 로그인 하지 않은 유저가 글쓰기 버튼을 클릭 하면 로그인 화면으로 이동 시킨다.
     * 로그인 한 유저는 세션에 저장되어있다. memberVO 변수로 값을 받고, 모델로 View 에 전달 시킨다.
     * 로그인 하지 않은 유저가 접근하려고 한다면 login으로 보낸다.
     * @param memberVO
     * @param model
     * @return
     */
    @GetMapping("/save")
    public String questionSave(@SessionAttribute(value = "user", required = false) MemberVO memberVO, Model model) {

        if (memberVO == null) {
            return "redirect:/login";
        }

        model.addAttribute("userId", memberVO.getId());
        model.addAttribute("inquiryMode", false);
        model.addAttribute("viewMode", "board");
        model.addAttribute("listPath", "/qna");
        model.addAttribute("returnPath", "/qna");
        return "qna/question-save";
    }

    @GetMapping("/inquiry/save")
    public String inquirySave(@SessionAttribute(value = "user", required = false) MemberVO memberVO,
                              Model model) {
        if (memberVO == null) {
            return "redirect:/login";
        }

        model.addAttribute("userId", memberVO.getId());
        model.addAttribute("inquiryMode", true);
        model.addAttribute("viewMode", "myInquiry");
        model.addAttribute("listPath", "/qna/my");
        model.addAttribute("returnPath", "/qna/my");
        return "qna/question-save";
    }

    @GetMapping("/{no}")
    public String detail(@PathVariable Long no, Model model,
                         @SessionAttribute(value = "user", required = false) MemberVO memberVO) throws SQLException {
        QuestionResponseDTO dto = questionService.findByNo(no);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }

        boolean inquiryMode = QuestionCategory.isInquiry(dto.getCategory());
        if (!inquiryMode && !QuestionCategory.isBoard(dto.getCategory())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        if (inquiryMode) {
			memberVO = currentSessionUser(memberVO);
            assertCanViewInquiry(dto, memberVO);
        }

        questionService.increaseViews(no);
        dto.setViews(dto.getViews() + 1);
        model.addAttribute("question", dto);
        String listPath = inquiryMode
                ? (isAdmin(memberVO) ? "/qna/inquiries" : "/qna/my")
                : "/qna";
        model.addAttribute("inquiryMode", inquiryMode);
        model.addAttribute("viewMode", inquiryMode
                ? (isAdmin(memberVO) ? "adminInquiry" : "myInquiry")
                : "board");
        model.addAttribute("listPath", listPath);
        model.addAttribute("returnPath", listPath);
        
        if (dto.getIdx() != null) {
	        	FileUploadVO fileVO = new FileUploadVO();
	        	fileVO.setIdx(dto.getIdx().intValue());
	        	fileVO = fileUploadService.doSelectOne(fileVO);
	        	
	        if (fileVO != null) {
	            String url = fileVO.getUrl();
	            model.addAttribute("img", url);
	            model.addAttribute("storedFileName", fileVO.getName());
	        }
        }
        
	     // 답변 등록 결과를 반환값으로 받아서 이용
        AnswerResponseDTO answerDto = inquiryMode ? answerService.findByNo(no) : null;
        model.addAttribute("answer", answerDto);
        model.addAttribute("answerId", memberVO == null ? null : memberVO.getId());
        
        return "qna/question-detail";
    }

    @GetMapping("/update/{no}")
    public String questionUpdate(@PathVariable Long no, Model model,
                                 @SessionAttribute("user") MemberVO memberVO) throws SQLException {
        QuestionResponseDTO dto = questionService.findByNo(no);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        if (!isAdmin(memberVO) && !dto.getId().equals(memberVO.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Question access denied");
        }
        boolean inquiryMode = QuestionCategory.isInquiry(dto.getCategory());
        if (!inquiryMode && !QuestionCategory.isBoard(dto.getCategory())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        model.addAttribute("question", dto);
        model.addAttribute("userId", dto.getId());
        String listPath = inquiryMode
                ? (isAdmin(memberVO) ? "/qna/inquiries" : "/qna/my")
                : "/qna";
        model.addAttribute("inquiryMode", inquiryMode);
        model.addAttribute("viewMode", inquiryMode
                ? (isAdmin(memberVO) ? "adminInquiry" : "myInquiry")
                : "board");
        model.addAttribute("listPath", listPath);
        model.addAttribute("returnPath", listPath);
        
        if (dto.getIdx() != null) {
	        	FileUploadVO fileVO = new FileUploadVO();
	        	fileVO.setIdx(dto.getIdx().intValue());
	        	fileVO = fileUploadService.doSelectOne(fileVO);
	        	
	        if (fileVO != null) {
	            String originFileName = fileVO.getName();
	            int originalNameStart = originFileName.indexOf('_');
	            String fileName = originalNameStart >= 0
	                    ? originFileName.substring(originalNameStart + 1)
	                    : originFileName;
	            model.addAttribute("originFileName", originFileName);
	            model.addAttribute("fileName", fileName);
	        }
        }
        
        return "qna/question-update";
    }
    
	@PostMapping("/fileUploaded")
	@ResponseStatus(HttpStatus.GONE)
	public void uploadFileGone() {
		// Attachments must be persisted with the question in one multipart request.
	}

	@DeleteMapping("/fileDelete")
	@ResponseStatus(HttpStatus.GONE)
	public void deleteFileGone() {
		// Direct attachment deletion is retired to prevent attach/delete races.
	}

	private boolean isAdmin(MemberVO memberVO) {
		return memberVO != null && memberVO.getGrade() == ADMIN_GRADE;
	}

    private void assertCanViewInquiry(QuestionResponseDTO question, MemberVO memberVO) {
        if (memberVO == null
                || (!isAdmin(memberVO) && !question.getId().equals(memberVO.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inquiry access denied");
        }
    }

	private MemberVO currentSessionUser(MemberVO sessionUser) throws SQLException {
		if (sessionUser == null || sessionUser.getId() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry access denied");
		}
		MemberVO lookup = new MemberVO();
		lookup.setId(sessionUser.getId());
		MemberVO currentUser = userService.selectUser(lookup);
		if (currentUser == null
				|| (currentUser.getGrade() != 1 && currentUser.getGrade() != ADMIN_GRADE)
				|| currentUser.getCredentialVersion() != sessionUser.getCredentialVersion()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry access denied");
		}
		currentUser.setPassword(null);
		return currentUser;
	}

    private void addViewMode(Model model, String viewMode, String listPath) {
        boolean mineOnly = "myInquiry".equals(viewMode);
        boolean adminInquiryMode = "adminInquiry".equals(viewMode);
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("listPath", listPath);
        model.addAttribute("inquiryMode", mineOnly || adminInquiryMode);
        model.addAttribute("mineOnly", mineOnly);
        model.addAttribute("adminInquiryMode", adminInquiryMode);
    }

    private void addListModel(Model model, int page, int size, int totalPages,
                              String category, String searchType, String keyword) {
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("category", category);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
    }

    private String normalizeBoardCategory(String category) {
        if (CATEGORY_NOTICE.equals(category) || CATEGORY_GENERAL.equals(category)) {
            return category;
        }
        return "";
    }

    private Integer boardCategoryCode(String category) {
        if (CATEGORY_NOTICE.equals(category)) {
            return QuestionCategory.NOTICE;
        }
        if (CATEGORY_GENERAL.equals(category)) {
            return QuestionCategory.BOARD_POST;
        }
        return null;
    }

    private String normalizeInquiryCategory(String category) {
        if (CATEGORY_ANSWERED.equals(category) || CATEGORY_WAITING.equals(category)) {
            return category;
        }
        return "";
    }

    private Integer inquiryCategoryCode(String category) {
        if (CATEGORY_ANSWERED.equals(category)) {
            return QuestionCategory.INQUIRY_ANSWERED;
        }
        if (CATEGORY_WAITING.equals(category)) {
            return QuestionCategory.INQUIRY_WAITING;
        }
        return null;
    }

    private String normalizeSearchType(String searchType) {
        if (SEARCH_TITLE.equals(searchType) || SEARCH_CONTENT.equals(searchType)) {
            return searchType;
        }
        return SEARCH_BOTH;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.codePointCount(0, normalized.length()) > MAX_KEYWORD_LENGTH) {
            return normalized.substring(0, normalized.offsetByCodePoints(0, MAX_KEYWORD_LENGTH));
        }
        return normalized;
    }

}
