package com.roadscanner.dao.upload;

import java.sql.SQLException;
import java.util.List;

import com.roadscanner.domain.upload.FileUploadVO;

public interface FileUploadDao {

	// 피드백 월별 그래프
	List<FileUploadVO> monthlyFeedback(FileUploadVO inVO) throws SQLException;

	// 피드백 누적 그래프, 표
	FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException;

	// 카테고리별 사진 목록 조회
	List<FileUploadVO> doRetrieveByCategory(FileUploadVO inVO) throws SQLException;

	// 업로드된 사진 전체 목록 조회 (카테고리: 10, 20, 30)
	List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException;

	// 사진 상세 조회
	FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException;
	
	// 사진 수정
	int doUpdate(FileUploadVO inVO) throws SQLException;

	// 사진 삭제
	int doDelete(FileUploadVO inVO) throws SQLException;

	// 사진 업로드
	int doSave(FileUploadVO inVO) throws SQLException;
}