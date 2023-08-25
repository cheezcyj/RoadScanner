package com.roadscanner.controller.user;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;

@Controller
public class WithdrawController {
    
    @Autowired
    UserService userService;
    
    final Logger LOG = LogManager.getLogger(getClass());

    // 탈퇴 페이지 이동
    @RequestMapping(value = "/withdraw", method = RequestMethod.GET)
    public String withdrawPageStart() {
    	
        LOG.debug("탈퇴 페이지 이동");
        return "/login/withdraw";
    }

    // 회원 탈퇴
    @RequestMapping(value = "/withdraw", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String withdraw(MemberVO user) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ WithdrawController withdraw()                          │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        // UserService에 비밀번호가 일치하는지 확인하는 메서드가 있다
        int withdrawStatus = userService.doWithdraw(user);
        
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
    
    //관리자의 강제 삭제
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String delete(MemberVO user) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ WithdrawController delete()                          │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        // UserService에 비밀번호가 일치하는지 확인하는 메서드가 있다
        int withdrawStatus = userService.delete(user);
        
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
    
    // 회원 정지
    @RequestMapping(value = "/forbidden", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String forbidden(MemberVO user) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ WithdrawController forbidden()                         │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
        
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
    
    // 정지 회원 해제
    @RequestMapping(value = "/clear", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String clear(MemberVO user) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ WithdrawController clear()                             │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
        
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