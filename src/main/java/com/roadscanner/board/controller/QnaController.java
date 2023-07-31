package com.roadscanner.board.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roadscanner.board.domain.Qna;
import com.roadscanner.board.service.QnaService;
import com.roadscanner.board.service.QnaServiceImpl;
import com.roadscanner.board.util.OracleDbUtil;

@WebServlet("/qna/*")
public class QnaController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private QnaService qnaService;

    @Override
    public void init() throws ServletException {
        Connection connection = null;
        try {
            connection = OracleDbUtil.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        qnaService = new QnaServiceImpl(connection);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            listQna(request, response);
        } else if (pathInfo.equals("/view")) {
            viewQna(request, response);
        } else if (pathInfo.equals("/write")) {
            showWriteForm(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/write")) {
            createQna(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void listQna(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Qna> qnaList = qnaService.getQnaList();
            request.setAttribute("qnaList", qnaList);
            request.getRequestDispatcher("/WEB-INF/views/qna/list.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void viewQna(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String qIdxStr = request.getParameter("qIdx");
        String rId = request.getParameter("rId");
        String uIdxStr = request.getParameter("uIdx");

        if (qIdxStr == null || rId == null || uIdxStr == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int qIdx = Integer.parseInt(qIdxStr);
            int uIdx = Integer.parseInt(uIdxStr);
            Qna qna = qnaService.getQna(qIdx, rId, uIdx);
            if (qna != null) {
                request.setAttribute("qna", qna);
                request.getRequestDispatcher("/WEB-INF/views/qna/view.jsp").forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showWriteForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Write form을 보여주는 코드 구현
        // 작성 폼 JSP 파일로 포워딩합니다.
        request.getRequestDispatcher("/WEB-INF/views/qna/write.jsp").forward(request, response);
    }

    private void createQna(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Create Qna 기능 구현
        String qTitle = request.getParameter("qTitle");
        String qContents = request.getParameter("qContents");
        String rId = request.getParameter("rId");
    
        // 필요한 유효성 검사 등의 로직을 추가할 수 있습니다.
        // (생략)
    
        try {
            // Qna 객체 생성
            Qna qna = new Qna();
            qna.setqTitle(qTitle);
            qna.setqContents(qContents);
            qna.setrId(rId);
        
            // QnaService를 통해 DB에 저장
            boolean success = qnaService.createQna(qna);
        
            if (success) {
                // 생성이 성공적으로 완료되면, 목록 페이지로 리다이렉트합니다.
                response.sendRedirect(request.getContextPath() + "/qna/");
            } else {
                // 생성에 실패한 경우, 에러 메시지 등을 설정하고 작성 폼으로 다시 이동할 수 있습니다.
                // (생략)
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
