package com.roadscanner.controller.user;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.domain.MemberVO;
import com.roadscanner.service.user.UserService;

@Controller
public class WithdrawController {
    
    @Autowired
    UserService userService;

    /**
     * 회원 탈퇴 페이지로 이동
     * @return 회원 탈퇴 페이지 뷰 이름
     */
    @RequestMapping(value = "/withdraw", method = RequestMethod.GET)
    public String withdrawPageStart() {
        System.out.println("회원 탈퇴 페이지로 이동...");
        return "withdraw";
    }

    /**
     * 회원 탈퇴 처리
     * @param user 회원 정보 (비밀번호가 담긴 객체)
     * @return JSON 형태의 응답 데이터
     * @throws SQLException 
     */
    @RequestMapping(value = "/withdraw", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String withdraw(MemberVO user) throws SQLException {
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│ withdraw()                                            │");
        System.out.println("└────────────────────────────────────────────────────────┘");
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        // UserService에 비밀번호가 일치하는지 확인하는 메서드가 있다
        int withdrawStatus = userService.deleteOne(user);
        
        if (withdrawStatus == 1) {
                message.setMsgId("10");
                message.setMsgContents("탈퇴되었습니다.");
        } else {
                message.setMsgId("20");
                message.setMsgContents("회원 탈퇴에 실패했습니다.");
        }
        
        jsonString = new Gson().toJson(message);
        return jsonString;
    }
    
    /**
     * 회원 정지 처리
     * @param user 회원 정보 (비밀번호가 담긴 객체)
     * @return JSON 형태의 응답 데이터
     * @throws SQLException 
     */
    @RequestMapping(value = "/forbidden", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String forbidden(MemberVO user) throws SQLException {
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│ forbidden()                                            │");
        System.out.println("└────────────────────────────────────────────────────────┘");
        
        String jsonString = "";
        MessageVO message = new MessageVO();

        int updateGrade = userService.forbiddenGrade(user);
        
        if (updateGrade == 1) {
                message.setMsgId("10");
                message.setMsgContents("회원이 정지되었습니다.");
        } else {
                message.setMsgId("20");
                message.setMsgContents("회원정지 처리에 실패했습니다.");
        }
        
        jsonString = new Gson().toJson(message);
        return jsonString;
    }
    
    /**
     * 회원 정지 해제 처리
     * @param user
     * @return
     * @throws SQLException
     */
    @RequestMapping(value = "/clear", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String clear(MemberVO user) throws SQLException {
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│ clear()                                            │");
        System.out.println("└────────────────────────────────────────────────────────┘");
        
        String jsonString = "";
        MessageVO message = new MessageVO();

        int updateGrade = userService.clearGrade(user);
        
        if (updateGrade == 1) {
                message.setMsgId("10");
                message.setMsgContents("회원의 정지가 해제되었습니다.");
        } else {
                message.setMsgId("20");
                message.setMsgContents("회원의 정지해제 처리가 실패했습니다.");
        }
        
        jsonString = new Gson().toJson(message);
        return jsonString;
    }
    
}