package com.roadscanner.cmn;

import java.util.List;

public interface BaseRepository<T> {

     // 기본 CRUD 제공

    /**
     * 데이터 저장
     * 저장이 몇개 되었는지 반환함.
     * @param vo
     * @return
     */
    Long save(T vo);

    /**
     * ID로 데이터 조회(단건)
     * @param no
     * @return
     */
    T findByNo(Long no);

    /**
     * 모든 데이터 조회
     * @return
     */
    List<T> findAll();

    void update(T vo);

    /**
     * 데이터 삭제
     * @param no
     */
    void delete(Long no);

}
