package com.roadscanner.dao.upload;

import java.sql.SQLException;
import java.util.List;

import com.roadscanner.domain.upload.FileUploadVO;
import org.apache.ibatis.annotations.Param;

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

	// 삭제 재시도 대기 중인 사진을 오래된 순서로 제한 조회
	List<FileUploadVO> findPendingDeletes(@Param("limit") int limit) throws SQLException;

	// 참조가 다시 생긴 삭제 대기 행을 기존 검토 상태로 조건부 복원
	int restorePendingDelete(@Param("idx") int idx,
			@Param("name") String name,
			@Param("pendingChecked") int pendingChecked,
			@Param("restoredChecked") int restoredChecked) throws SQLException;

	// 사진 업로드
	int doSave(FileUploadVO inVO) throws SQLException;
}
