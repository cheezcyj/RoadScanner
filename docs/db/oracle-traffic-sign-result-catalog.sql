-- RoadScanner recovered GTSRB result catalog (2026-08-02).
-- Apply before enabling the external ML service. Unverified legacy reference-image URLs are reset.
-- Re-running this script is safe.

DECLARE
    PROCEDURE upsert_result(
        p_no RESULT_IMAGE.no%TYPE,
        p_name RESULT_IMAGE.name%TYPE,
        p_content RESULT_IMAGE.content%TYPE
    ) IS
    BEGIN
        UPDATE RESULT_IMAGE
           SET name = p_name,
               content = p_content,
               url = 'none'
         WHERE no = p_no;
        IF SQL%ROWCOUNT = 0 THEN
            INSERT INTO RESULT_IMAGE (no, name, content, url)
            VALUES (p_no, p_name, p_content, 'none');
        END IF;
    END;
BEGIN
    upsert_result(1, 'Speed limit (20 km/h)', '최고속도 20km/h');
    upsert_result(2, 'Speed limit (30 km/h)', '최고속도 30km/h');
    upsert_result(3, 'Speed limit (50 km/h)', '최고속도 50km/h');
    upsert_result(4, 'Speed limit (60 km/h)', '최고속도 60km/h');
    upsert_result(5, 'Speed limit (70 km/h)', '최고속도 70km/h');
    upsert_result(6, 'Speed limit (80 km/h)', '최고속도 80km/h');
    upsert_result(7, 'End of speed limit (80 km/h)', '최고속도 80km/h 제한 해제');
    upsert_result(8, 'Speed limit (100 km/h)', '최고속도 100km/h');
    upsert_result(9, 'Speed limit (120 km/h)', '최고속도 120km/h');
    upsert_result(10, 'No passing', '앞지르기 금지');
    upsert_result(11, 'No passing for vehicles over 3.5 t', '3.5t 초과 차량의 앞지르기 금지');
    upsert_result(12, 'Right-of-way at the next intersection', '다음 교차로 통행 우선');
    upsert_result(13, 'Priority road', '우선도로');
    upsert_result(14, 'Yield', '양보');
    upsert_result(15, 'Stop', '정지 후 양보');
    upsert_result(16, 'No vehicles', '모든 차량 통행 금지');
    upsert_result(17, 'Vehicles over 3.5 t prohibited', '3.5t 초과 자동차 통행 금지');
    upsert_result(18, 'No entry', '진입 금지');
    upsert_result(19, 'General caution', '기타 위험 주의');
    upsert_result(20, 'Dangerous curve to the left', '좌측 위험 커브');
    upsert_result(21, 'Dangerous curve to the right', '우측 위험 커브');
    upsert_result(22, 'Double curve', '연속 커브');
    upsert_result(23, 'Bumpy road', '노면 요철 주의');
    upsert_result(24, 'Slippery road', '미끄러운 도로 주의');
    upsert_result(25, 'Road narrows on the right', '우측 도로 폭 감소');
    upsert_result(26, 'Road work', '도로 공사 주의');
    upsert_result(27, 'Traffic signals', '신호등 주의');
    upsert_result(28, 'Pedestrians', '보행자 주의');
    upsert_result(29, 'Children crossing', '어린이 주의');
    upsert_result(30, 'Bicycles crossing', '자전거 통행 주의');
    upsert_result(31, 'Snow or ice', '눈길 또는 빙판길 주의');
    upsert_result(32, 'Wild animals crossing', '야생동물 출현 주의');
    upsert_result(33, 'End of all speed and passing limits', '모든 구간 제한속도·앞지르기 금지 해제');
    upsert_result(34, 'Turn right ahead', '우회전 지시');
    upsert_result(35, 'Turn left ahead', '좌회전 지시');
    upsert_result(36, 'Ahead only', '직진 지시');
    upsert_result(37, 'Go straight or right', '직진 또는 우회전');
    upsert_result(38, 'Go straight or left', '직진 또는 좌회전');
    upsert_result(39, 'Keep right', '우측 통과 지시');
    upsert_result(40, 'Keep left', '좌측 통과 지시');
    upsert_result(41, 'Roundabout mandatory', '회전교차로 통행');
    upsert_result(42, 'End of no passing', '앞지르기 금지 해제');
    upsert_result(43, 'End of no passing for vehicles over 3.5 t', '3.5t 초과 차량 앞지르기 금지 해제');
    upsert_result(44, 'Unknown / low-confidence result', '인식 불가 — 표지판을 가깝게 잘라 다시 시도해 주세요');
END;
/

COMMIT;
