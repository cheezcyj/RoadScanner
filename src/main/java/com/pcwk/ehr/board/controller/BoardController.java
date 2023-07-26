package com.pcwk.ehr.board.controller;

import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.pcwk.ehr.board.domain.BoardVO;
import com.pcwk.ehr.board.service.BoardService;
import com.pcwk.ehr.board.domain.DTO;
import com.pcwk.ehr.cmn.MessageVO;
import com.pcwk.ehr.board.domain.PcwkLoger;
import com.pcwk.ehr.cmn.StringUtil;
import com.pcwk.ehr.code.domain.CodeVO;
import com.pcwk.ehr.code.service.CodeService;
import com.pcwk.ehr.user.domain.UserVO;

@Controller("boardController")
@RequestMapping("board")
public class BoardController implements PcwkLoger {
	@Autowired
	BoardService boardService;

	@Autowired
	CodeService codeService;

	public BoardController() {
	
	}
	
	//ajax json
	@RequestMapping(value="/doUpdate.do",method = RequestMethod.POST
			, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doUpdate(BoardVO inVO) throws SQLException {
		String jsonString = "";
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doUpdate                      │");
		LOG.debug("│inVO                          │" + inVO);
		LOG.debug("└──────────────────────────────┘");		
		
		int flag = this.boardService.doUpdate(inVO);
		
		String message = "";
		if(1 == flag) {
			message = inVO.getTitle()+"이 수정 되었습니다.";
		}else {
			message = "수정 실패";
		}
		
		jsonString = StringUtil.validMessageToJson(flag+"", message);
		LOG.debug("│jsonString                          │" + jsonString);		
		return jsonString;
	}
	
	@RequestMapping(value = "/doDelete.do", method = RequestMethod.GET
			, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doDelete(BoardVO inVO) throws SQLException {
		String jsonString = "";
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doDelete                      │");
		LOG.debug("│inVO                          │" + inVO);
		LOG.debug("└──────────────────────────────┘");
		
		
		int flag = boardService.doDelete(inVO);
		
		String message = "";
		if (1 == flag) {// 삭제 성공
			message = inVO.getSeq() + " 삭제 성공";
		} else {// 등록실패
			message = inVO.getSeq() + " 삭제 실패";
		}

		jsonString = StringUtil.validMessageToJson(flag + "", message);
		LOG.debug("│jsonString                          │" + jsonString);
		
	    return 	jsonString;
	}	
	

	@RequestMapping("/doSelectOne.do")
	public String doSelectOne(BoardVO inVO,Model model, HttpSession httpSession) throws SQLException {
		String view = "board/board_mng";
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doSelectOne                   │");
		LOG.debug("│inVO                          │" + inVO);
		LOG.debug("└──────────────────────────────┘");

		/*
		 * ┌──────────────────────────────┐ │doSelectOne │ │inVO │BoardVO [seq=393,
		 * div=10
		 */
        //등록자 ID를 Session에서 추출
		UserVO userVO = (UserVO) httpSession.getAttribute("user");
		LOG.debug("│userVO                          │" + userVO);
		inVO.setModId(userVO.getUserId());    
		
		BoardVO outVO = boardService.doSelectOne(inVO);
		model.addAttribute("outVO",outVO);
		model.addAttribute("inVO",inVO);
		
		return view;
	}

	@RequestMapping(value = "/doSave.do", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doSave(BoardVO inVO) throws SQLException {
		String jsonString = "";
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doSave                        │");
		LOG.debug("│inVO                          │" + inVO);
		LOG.debug("└──────────────────────────────┘");

		// 제목:10
		if (null != inVO && inVO.getTitle().equals("")) {
			return StringUtil.validMessageToJson("10", "제목을 입력 하세요");
		}

		// 등록자:20
		if (null != inVO && inVO.getRegId().equals("") && null == inVO.getRegId()) {
			return StringUtil.validMessageToJson("20", "등록자를 입력 하세요");
		}

		// 내용:30
		if (null != inVO && inVO.getContents().equals("")) {
			return StringUtil.validMessageToJson("30", "내용을 입력 하세요");
		}

		// 서비스 호출
		int flag = this.boardService.doSave(inVO);

		String message = "";
		if (1 == flag) {// 등록 성공
			message = inVO.getTitle() + " 등록 성공";
		} else {// 등록실패
			message = inVO.getTitle() + " 등록실패";
		}

		jsonString = StringUtil.validMessageToJson(flag + "", message);
		LOG.debug("│jsonString                          │" + jsonString);
		return jsonString;
	}

	// ${CP}/board/doMoveToReg.do
	// http://localhost:8080/ehr/board/doMoveToReg.do?div=10
	// http://localhost:8080/ehr/board/doMoveToReg.do?pageNo=1&div=10&searchDiv=20&searchWord=%EB%82%B4&pageSize=20
	@RequestMapping("/doMoveToReg.do")
	public String doMoveToReg(BoardVO inVO, Model model) throws SQLException {
		String view = "board/board_reg";
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doMoveToReg                   │");
		LOG.debug("│inVO                          │" + inVO);
		LOG.debug("└──────────────────────────────┘");

		model.addAttribute("inVO", inVO);
		return view;
	}

	@RequestMapping("/boardView.do")
	public String boardView(BoardVO inVO, Model model) throws SQLException {
		String viewPage = "board/board_list";

		// page번호
		if (null != inVO && inVO.getPageNo() == 0) {
			inVO.setPageNo(1);
		}

		// pageSize
		if (null != inVO && inVO.getPageSize() == 0) {
			inVO.setPageSize(10);
		}

		// searchWord
		if (null != inVO && null == inVO.getSearchWord()) {
			inVO.setSearchWord("");
		}

		// searchDiv
		if (null != inVO && null == inVO.getSearchDiv()) {
			inVO.setSearchDiv("");
		}

		//
		LOG.debug("inVO:" + inVO);

		// 코드조회: 검색코드
		CodeVO codeVO = new CodeVO();
		codeVO.setCodeId("BOARD_SEARCH");
		List<CodeVO> searchList = codeService.doRetrieve(codeVO);
		model.addAttribute("searchList", searchList);

		// 코드조회: 페이지 사이즈
		codeVO.setCodeId("CMN_PAGE_SIZE");
		List<CodeVO> pageSizeList = codeService.doRetrieve(codeVO);
		model.addAttribute("pageSizeList", pageSizeList);

		List<BoardVO> list = this.boardService.doRetrieve(inVO);
		model.addAttribute("list", list);
		
		//총글수
		int totalCnt = 0;
		if(null !=list && list.size() >0 ) {
			totalCnt = list.get(0).getTotalCnt();
			LOG.debug("totalCnt:" + totalCnt);
		}
		
		model.addAttribute("totalCnt", totalCnt);
		
		
		
		model.addAttribute("inVO", inVO);
		return viewPage;
	}

}
